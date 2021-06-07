variable "project-config-map" {
  type = "map"
  default = {
    //--------------------------------------- GKE ----------------------------------------//
    // When turned on, use the following command to configure kubectl:
    // gcloud container clusters get-credentials green-1-project-example-gke --zone europe-west1-b --project green-1-project-example
    gke-on                       = "0"
    gke-node-type                = "n1-standard-4"
    gke-node-count               = "2"
    gke-node-pool                = "0"
    gke-node-pool-count          = "2"

    //------------------------------------- Elastic ---------------------------------------//
    elastic-on                   = "0"
    // Currently we wouldn't be using any dedicated Master nodes for ES
    elastic-master-node-type     = "n1-standard-2"
    elastic-master-node-count    = "0"
    elastic-worker-node-type     = "n1-highmem-8"
    elastic-worker-node-count    = "1"

    // Change 'persistent-data-disk-size-gb' > '0' if you don't want to use Google Scratch Disk (375 GB SSD)
    // It will then create a persistent disk to store ES data with the size specified.
    // Example, persistent-data-disk-size-gb='500' to create a 500 GB persistent Data disk
    persistent-data-disk-size-gb = "0"

    kibana-on                    = "0"
    kibana-node-type             = "n1-highmem-4"
  }
}

//-----------------------------------------------------------------------------//
// These variables are required if using the legacy module for shared dataproc //
//-----------------------------------------------------------------------------//

/*
// Variables need to be set via environment variables with the prefix TF_VAR_ e.g.
// ~.bashrc
// export TF_VAR_ssh_username="****"

// This should be your username with -admin appended e.g.
// export TF_VAR_ssh_username=$USER-admin
variable "ssh_username" {}

// This should be your public key to be deployed onto terraformed infrastructure e.g.
// export TF_VAR_ssh_authorized_keys=ssh-rsa *** my-user-20180101
variable "ssh_authorized_keys" {}

variable "legacy-project-config-map" {
  type = "map"
  default = {
    dataproc-on                  = "0"
    dataproc-master-node-type    = "n1-highmem-4"
    dataproc-worker-node-count   = "2"
    dataproc-worker-node-type    = "n1-highmem-8"
    dataproc-pre-emptible-worker-node-count = "2"
    dataproc-worker-local-ssds   = "1"

    elastic-on                   = "0"
    // Currently we wouldn't be using any dedicated Master nodes for ES
    elastic-master-node-type     = "n1-standard-2"
    elastic-master-node-count    = "0"
    elastic-worker-node-type     = "n1-highmem-8"
    elastic-worker-node-count    = "1"

    // Change 'persistent-data-disk-size-gb' > '0' if you don't want to use Google Scratch Disk (375 GB SSD)
    // It will then create a persistent disk to store ES data with the size specified.
    // Example, persistent-data-disk-size-gb='500' to create a 500 GB persistent Data disk
    persistent-data-disk-size-gb = "0"

    kibana-on                    = "0"
    kibana-node-type             = "n1-highmem-4"

    gke-on                       = "0"
    gke-node-type                = "n1-standard-4"
    gke-node-count               = "2"
    gke-node-pool                = "0"
    gke-node-pool-count          = "2"
  }
}
*/