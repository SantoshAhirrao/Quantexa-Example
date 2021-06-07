# gcloud auth application-default login
provider "google" {
  project = "green-1-project-example"
  region  = "${data.terraform_remote_state.project-core.region}"
}

# gcloud auth application-default login
terraform {
  backend "gcs" {
    bucket = "quantexa-green-1-project-example-terraform"
    prefix = "project"
  }
}

data "terraform_remote_state" "project-core" {
  backend = "gcs"
  config {
    bucket = "quantexa-green-1-project-example-managed-infrastructure-state"
    prefix = "project-core"
  }
}
