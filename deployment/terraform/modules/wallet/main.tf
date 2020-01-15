resource "google_compute_address" "wallet-external" {
  name = "${var.external-ip-name}"
  address_type = "EXTERNAL"
}

resource "random_string" "sql-password" {
  length = 16
  special = false
}

resource "google_sql_database" "wallet" {
  depends_on = ["google_sql_user.wallet"]
  name      = "${var.database-name}"
  instance  = "${var.sql-instance-name}"
}

resource "google_sql_user" "wallet" {
  name     = "${var.database-user}"
  instance = "${var.sql-instance-name}"
  password = "${random_string.sql-password.result}"
}

resource "kubernetes_deployment" "wallet" {
  provider = "kubernetes"
  depends_on = ["google_sql_user.wallet"]
  metadata {
    name = "wallet"
    namespace = "${var.namespace}"
    labels {
      app = "wallet"
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels {
        app = "wallet"
      }
    }
    template {
      metadata {
        labels {
          app = "wallet"
        }
      }
      spec {
        node_selector {
          type = "default"
        }
        image_pull_secrets {
          name = "${var.image-pull-secret}"
        }
        volume {
          name = "cloudsql-instance-credentials"
          secret {
            secret_name = "cloud-sql-sa-key"
          }
        }
        container {
          name = "cloudsql-proxy"
          image = "gcr.io/cloudsql-docker/gce-proxy:1.11"
          command = ["/cloud_sql_proxy", "-instances=${var.sql-connection-name}=tcp:5432", "-credential_file=/secrets/cloudsql/credentials.json"]
          security_context {
            run_as_user = 2
            privileged = false
            allow_privilege_escalation = false
          }
          volume_mount {
            name = "cloudsql-instance-credentials"
            mount_path = "/secrets/cloudsql"
            read_only = true
          }
          resources {
            limits {
              memory = "512Mi"
              cpu = 0.5
            }
            requests {
              memory = "256Mi"
              cpu = 0.2
            }
          }
        }
        container {
          name = "wallet"
          image = "zmlp/wallet:${var.container-tag}"
          image_pull_policy = "Always"
          liveness_probe = {
            initial_delay_seconds = 30
            period_seconds = 5
            http_get {
              scheme = "HTTP"
              path = "/api/v1/health/"
              port = "80"
            }
          }
          readiness_probe = {
            initial_delay_seconds = 30
            period_seconds = 30
            http_get {
              scheme = "HTTP"
              path = "/api/v1/health/"
              port = "80"
            }
          }
          port {
            container_port = "80"
          }
          resources {
            limits {
              memory = "512Mi"
              cpu = 1.0
            }
            requests {
              memory = "256Mi"
              cpu = 0.2
            }
          }
          env = [
            {
              name = "PG_HOST"
              value = "localhost"
            },
            {
              name = "PG_PASSWORD"
              value = "${random_string.sql-password.result}"
            },
            {
              name = "ARCHIVIST_URL"
              value = "${var.archivist-url}"
            },
            {
              name = "SMTP_PASSWORD"
              value = "${var.smtp-password}"
            },
            {
              name = "GOOGLE_OAUTH_CLIENT_ID"
              value = "${var.google-oauth-client-id}"
            },
            {
              name = "FRONTEND_SENTRY_DSN"
              value = "${var.frontend-sentry-dsn}"
            }
          ]
        }
      }
    }
  }
}

resource "kubernetes_service" "wallet" {
  metadata {
    name = "wallet-service"
    namespace = "${var.namespace}"
    labels {
      app = "wallet"
    }
  }
  spec {
    port {
      name = "http"
      protocol = "TCP"
      port = 80
    }
    selector {
      app = "wallet"
    }
    type = "NodePort"
  }
}

resource "google_compute_managed_ssl_certificate" "default" {
  provider = "google-beta"
  name = "wallet-cert"
  managed {
    domains = ["wallet.zmlp.zorroa.com"]
  }
}

resource "kubernetes_ingress" "wallet" {
  metadata {
    name = "wallet-ingress"
    namespace = "${var.namespace}"
    annotations {
      "kubernetes.io/ingress.allow-http" = "false"
      "ingress.gcp.kubernetes.io/pre-shared-cert" = "${google_compute_managed_ssl_certificate.default.name}"
      "kubernetes.io/ingress.global-static-ip-name" = "${google_compute_address.wallet-external.name}"
    }
  }
  spec {
    backend {
      service_name = "wallet-service"
      service_port = 80
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "wallet" {
  provider = "kubernetes"
  metadata {
    name = "wallet-hpa"
    namespace = "${var.namespace}"
    labels {
      app = "wallet"
    }
  }
  spec {
    max_replicas = 10
    min_replicas = 2
    scale_target_ref {
      api_version = "apps/v1"
      kind = "Deployment"
      name = "wallet"
    }
    target_cpu_utilization_percentage = 80
  }
}
