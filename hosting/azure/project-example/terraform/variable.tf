// Variables need to be set via environment variables with the prefix TF_VAR_ e.g.
// ~.bashrc
// export TF_VAR_kube_id="****"

// Azure Service Account for AKS to use on your behalf - provided by IT Ops
variable "kube_id" {}
variable "kube_secret" {}

// This should be your username with -admin appended e.g.
// export TF_VAR_ssh_username=$USER-admin
variable "ssh_username" {}
// This should be your public key to be deployed onto terraformed infrastructure e.g.
// export TF_VAR_ssh_keys=ssh-rsa *** my-user-20180101
variable "ssh_authorized_keys" {}

// This will be prompted at run-time
variable "hdi_ambari_username" {}
variable "hdi_ambari_password" {}

variable "project-config-map" {
  type = "map"
  default = {
    //When setting hdi to 0, you must go and manually delete due to being a template
    hdi-on = "0"
    hdi-master-node-type = "Standard_D12_v2"
    hdi-worker-node-type = "Standard_D12_v2"
    hdi-worker-node-count = "2"
    elastic-on = "0"
    // Currently we wouldn't be using any dedicated Master nodes for ES
    elastic-master-node-type = "Standard_DS2_v2"
    elastic-master-node-count = "0"
    elastic-worker-node-type = "Standard_L8S"
    elastic-worker-node-count = "3"
    elastic-persistsent-data-disk-size-gb = "1023"
    kibana-on = "0"
    kibana-node-type = "Standard_E8s_v3"
    /*
      AKS Manual Steps after creation:
      1. Access generated MC-AKS resource group and attach Route table to project AKS subnet
    */
    aks-on = "0"
    aks-node-type = "Standard_DS4_v2"
    aks-node-count = "2"
  }
}
