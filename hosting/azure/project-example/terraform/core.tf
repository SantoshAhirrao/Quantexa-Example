module "hdinsight" {
  source = "git::ssh://git@gitlab.com/Quantexa/dev-ops/terraform-modules/Azure-Hadoop.git?ref=0.3.0"

  // When turned to false you must go and delete the cluster manually as HDInsight is launched via a template and
  // thus not managed by terraform
  clusterOn = "${var.project-config-map["hdi-on"]}"

  region = "${data.terraform_remote_state.project-core.region}"

  hdInsightProjectResourceGroup = "${data.terraform_remote_state.project-core.hdi-rg-name}"

  subnetID               = "${data.terraform_remote_state.project-core.hdi-subnet-id}"
  subnetVirtualNetworkID = "${data.terraform_remote_state.project-core.quantexa-domain-vnet-id}"

  sshUsername  = "${var.ssh_username}"
  sshPublicKey = "${var.ssh_authorized_keys}"

  location    = "${lower(replace(data.terraform_remote_state.project-core.region, " ", ""))}"
  projectName = "${data.terraform_remote_state.project-core.project-name}"

  clusterLoginUserName = "${var.hdi_ambari_username}"
  clusterLoginPassword = "${var.hdi_ambari_password}"

  clusterMasterNodeType  = "${var.project-config-map["hdi-master-node-type"]}"
  clusterWorkerNodeCount = "${var.project-config-map["hdi-worker-node-count"]}"
  clusterWorkerNodeType  = "${var.project-config-map["hdi-worker-node-type"]}"

  hdInsightStorageAccountName      = "${data.terraform_remote_state.project-core.hdi-sto-name}"
  hdInsightStorageAccountAccessKey = "${data.terraform_remote_state.project-core.hdi-sto-access}"
}

module "elastic_no_loadbalancer" {
  source = "git::ssh://git@gitlab.com/Quantexa/dev-ops/terraform-modules/Azure-ELK.git?ref=0.2.5"

  clusterOn = "${var.project-config-map["elastic-on"]}"

  region = "${data.terraform_remote_state.project-core.region}"

  elkSubnetID = "${data.terraform_remote_state.project-core.elk-subnet-id}"

  elasticResourceGroup = "${data.terraform_remote_state.project-core.elk-rg-name}"

  sshUsername  = "${var.ssh_username}"
  sshPublicKey = "${var.ssh_authorized_keys}"

  projectName = "${data.terraform_remote_state.project-core.project-name}"

  clusterMasterNodeCount = "${var.project-config-map["elastic-master-node-count"]}"
  clusterMasterNodeType = "${var.project-config-map["elastic-master-node-type"]}"

  clusterDataNodeCount = "${var.project-config-map["elastic-worker-node-count"]}"
  clusterDataNodeType  = "${var.project-config-map["elastic-worker-node-type"]}"

  kibanaVM     = "${var.project-config-map["kibana-on"]}"
  kibanaVMType = "${var.project-config-map["kibana-node-type"]}"

  kibanaNSGId  = "${element(concat(azurerm_network_security_group.elastic-kibana-nsg.*.id, list("")), 0)}"
  elkDataNSGId = "${element(concat(azurerm_network_security_group.elastic-data-nsg.*.id, list("")), 0)}"

  diskSize = 20
}

/*
  Manual Steps after creation:
  1. Access generated MC-AKS resource group and attach Route table to project AKS subnet
*/
resource "azurerm_kubernetes_cluster" "aks" {
  count = "${var.project-config-map["aks-on"]}"

  location            = "${data.terraform_remote_state.project-core.region}"
  name                = "aks-${data.terraform_remote_state.project-core.project-name}"
  resource_group_name = "${data.terraform_remote_state.project-core.aks-rg-name}"

  dns_prefix         = "aks-${data.terraform_remote_state.project-core.project-name}"
  kubernetes_version = "1.11.5"

  "agent_pool_profile" {
    name           = "core"
    vm_size        = "${var.project-config-map["aks-node-type"]}"
    count          = "${var.project-config-map["aks-node-count"]}"
    vnet_subnet_id = "${data.terraform_remote_state.project-core.aks-subnet-id}"
    os_disk_size_gb = "30"
  }

  "linux_profile" {
    admin_username = "${var.ssh_username}"

    "ssh_key" {
      key_data = "${var.ssh_authorized_keys}"
    }
  }

  "service_principal" {
    client_id     = "${var.kube_id}"
    client_secret = "${var.kube_secret}"
  }
}
