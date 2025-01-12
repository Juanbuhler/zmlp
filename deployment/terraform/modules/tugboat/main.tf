resource "google_project_service" "cloudbuild" {
  service            = "cloudbuild.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_iam_member" "cloudbuilder" {
  project    = var.project
  role       = "roles/run.admin"
  member     = "serviceAccount:${var.project-number}@cloudbuild.gserviceaccount.com"
  depends_on = [google_project_service.cloudbuild]
}

resource "google_project_iam_member" "cloudbuilder-role-2" {
  project    = var.project
  role       = "roles/iam.serviceAccountUser"
  member     = "serviceAccount:${var.project-number}@cloudbuild.gserviceaccount.com"
  depends_on = [google_project_service.cloudbuild]
}

resource "google_pubsub_subscription" "tugboat-model-events" {
  name  = "tugboat-model-events"
  topic = var.pubsub-topic
}

resource "google_service_account" "boon-function" {
  project      = var.project
  account_id   = "boon-function"
  display_name = "Boon Function Service Account"
}

resource "google_service_account" "tugboat" {
  project      = var.project
  account_id   = "zmlp-tugboat"
  display_name = "ZMLP Tugboat"
}

resource "google_project_iam_custom_role" "tugboat" {
  project     = var.project
  role_id     = "tugboat"
  title       = "ZMLP Tugboat Role"
  description = "Role assigned to the service account used by Tugboat."
  permissions = [
    "run.services.delete",
    "run.services.list",
    "run.services.get",
    "cloudbuild.builds.create",
    "cloudbuild.builds.get",
    "cloudbuild.builds.list",
    "cloudbuild.builds.update",
    "remotebuildexecution.blobs.get",
    "pubsub.snapshots.seek",
    "pubsub.subscriptions.consume",
    "pubsub.topics.attachSubscription",
    "storage.buckets.create",
    "storage.buckets.get",
    "storage.buckets.list",
    "storage.buckets.update",
    "storage.objects.create",
    "storage.objects.delete",
    "storage.objects.get",
    "storage.objects.list",
    "storage.objects.update",
    "logging.logEntries.create",
    "artifactregistry.repositories.list",
    "artifactregistry.repositories.get",
    "artifactregistry.repositories.downloadArtifacts",
    "artifactregistry.files.list",
    "artifactregistry.files.get",
    "artifactregistry.packages.list",
    "artifactregistry.packages.get",
    "artifactregistry.tags.list",
    "artifactregistry.tags.get",
    "artifactregistry.versions.list",
    "artifactregistry.versions.get",
    "pubsub.topics.create",
    "pubsub.topics.publish",
    "source.repos.get",
    "source.repos.list"
  ]
}

resource "google_container_node_pool" "tugboat" {
  name               = "tugboat"
  cluster            = var.container-cluster-name
  initial_node_count = 1
  autoscaling {
    max_node_count = 3
    min_node_count = 1
  }
  management {
    auto_repair  = true
    auto_upgrade = true
  }
  node_config {
    preemptible     = true
    machine_type    = "n1-standard-1"
    service_account = google_service_account.tugboat.email
    oauth_scopes    = ["https://www.googleapis.com/auth/cloud-platform"]
    labels = {
      type = "tugboat"
    }
    taint {
      effect = "NO_SCHEDULE"
      key    = "tugboat"
      value  = "false"
    }
  }
  lifecycle {
    ignore_changes = [
      initial_node_count,
      autoscaling[0].min_node_count,
      autoscaling[0].max_node_count
    ]
  }
  depends_on = [google_service_account.tugboat]
}

resource "google_project_iam_member" "tugboat" {
  project    = var.project
  role       = google_project_iam_custom_role.tugboat.id
  member     = "serviceAccount:${google_service_account.tugboat.email}"
  depends_on = [google_project_iam_custom_role.tugboat, google_service_account.tugboat]
}

resource "kubernetes_deployment" "tugboat" {
  provider = kubernetes
  metadata {
    name = "tugboat"
    labels = {
      app = "tugboat"
    }
  }
  spec {
    selector {
      match_labels = {
        app = "tugboat"
      }
    }
    template {
      metadata {
        labels = {
          app = "tugboat"
        }
      }
      spec {
        node_selector = {
          type = "tugboat"
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        toleration {
          key      = "tugboat"
          operator = "Equal"
          value    = "false"
          effect   = "NoSchedule"
        }
        volume {
          name = "certs"
          host_path {
            path = "/etc/ssl/certs"
          }
        }
        container {
          name  = "tugboat"
          image = "boonai/tugboat:${var.container-tag}"
          env {
            name  = "GCLOUD_PROJECT"
            value = var.project
          }
          env {
            name  = "PORT"
            value = "9393"
          }
          env {
            name  = "BOONAI_ENV"
            value = var.environment
          }
          env {
            name = "BOONAI_FUNC_SVC_ACCOUNT"
            value = google_service_account.boon-function.email
          }
          volume_mount {
            mount_path = "/etc/ssl/certs"
            name       = "certs"
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "9393"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "9393"
            }
          }
        }
      }
    }
  }
}
