output "ip-address" {
  value = kubernetes_service.metrics.spec[0].cluster_ip
}

output "pg_password" {
  value = random_password.sql-password.result
}

