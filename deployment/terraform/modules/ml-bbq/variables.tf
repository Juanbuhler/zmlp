variable "image-pull-secret" {
}

variable "auth-server-url" {
}

variable "container-tag" {
  default = "latest"
}

variable "memory-request" {
  default = "4Gi"
}

variable "memory-limit" {
  default = "8Gi"
}

variable "cpu-request" {
  default = 2
}

variable "cpu-limit" {
  default = 3
}

variable "minimum-replicas" {
  default = 1
}

variable "maximum-replicas" {
  default = 2
}

variable "rollout-strategy" {
  default = "RollingUpdate"
}

variable "ip-address" {
  default = "10.3.240.107"
}

variable "namespace" {
  default = "default"
}
