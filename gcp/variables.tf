variable "project_id" {
  description = "this is GCP trust agent project"
  type = string
}

variable "region" {
  description = "this is the deafult region"
  type =  string
}

variable "environment" {
  default = "This is the dev env"
  type = string
}