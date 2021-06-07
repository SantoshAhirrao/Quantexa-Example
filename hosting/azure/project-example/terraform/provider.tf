//az login
provider "azurerm" {
  subscription_id = "1ca9485d-0646-4a81-8e65-749b3591d1c7"
  version         = "1.8.0"
}

// gcloud auth application-default login
terraform {
  backend "gcs" {
    bucket = "quantexa-project-example-terraform"
    prefix = "project/terraform.tfstate"
  }
}

data "terraform_remote_state" "project-core" {
  backend = "gcs"

  config {
    bucket = "quantexa-project-example-terraform"
    prefix = "project-core/terraform.tfstate"
  }
}
