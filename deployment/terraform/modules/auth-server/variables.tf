variable "image-pull-secret" {}
variable "sql-instance-name" {}
variable "sql-connection-name" {}
variable "inception-key-b64" {}
variable "system-bucket" {}

variable "container-tag" {default = "development"}
variable "namespace" {default = "default"}
variable "ip-address" {default = "10.3.240.101"}
variable "database-name" {default = "zorroa"}
variable "database-user" {default = "zorroa"}
