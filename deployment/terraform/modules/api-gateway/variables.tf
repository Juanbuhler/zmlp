variable "image-pull-secret" {
}

variable "archivist_host" {
}

variable "auth_server_host" {
}

variable "ml_bbq_host" {
}

variable "domain" {
}

variable "container-cluster-name" {
}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "external-ip-name" {
  default = "api-gateway-external-ip"
}

