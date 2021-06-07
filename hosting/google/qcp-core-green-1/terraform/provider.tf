# gcloud auth application-default login
provider "google" {
  project = "qcp-core-green-1"
  region  = "${data.terraform_remote_state.project-core.region}"
}

# gcloud auth application-default login
terraform {
  backend "gcs" {
    bucket = "quantexa-qcp-core-green-1-terraform"
    prefix = "project"
  }
}

data "terraform_remote_state" "project-core" {
  backend = "gcs"
  config {
    bucket = "quantexa-qcp-core-green-1-terraform"
    prefix = "project-core"
  }
}
