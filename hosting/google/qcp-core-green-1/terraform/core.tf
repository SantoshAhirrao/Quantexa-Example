module "elastic" {
  source = "git::ssh://git@gitlab.com/Quantexa/dev-ops/terraform-modules/Google-ELK.git?ref=0.3.4"

  projectName = "${data.terraform_remote_state.project-core.project_id}"
  clusterOn   = "${var.project-config-map["elastic-on"]}"
  zone        = "${data.terraform_remote_state.project-core.zone}"
  elkSubnetID = "${data.terraform_remote_state.project-core.ela_subnet_self_link}"

  clusterMasterNodeCount = "${var.project-config-map["elastic-master-node-count"]}"
  clusterMasterNodeType  = "${var.project-config-map["elastic-master-node-type"]}"

  clusterDataNodeCount = "${var.project-config-map["elastic-worker-node-count"]}"
  clusterDataNodeType  = "${var.project-config-map["elastic-worker-node-type"]}"

  //  Change it to True if you want the OS Disk for Elastic nodes to be SSDs
  osDiskSSD                = false
  persistentLogDiskSizeGB  = 20
  persistentDataDiskSizeGB = "${var.project-config-map["persistent-data-disk-size-gb"]}"

  kibanaVM     = "${var.project-config-map["kibana-on"]}"
  kibanaVMType = "${var.project-config-map["kibana-node-type"]}"

  elasticServiceAccount = "${data.terraform_remote_state.project-core.elastic_svc_account}"
  blockProjectSshKeys   = true

  // Add this to your project if it is within service perimeter
//  elasticConfigTags = {
//    es_shard_awareness              = "False"
//    es_config-node-name             = "node1"
//    es_config-http-port             = 9200
//    es_config-transport-tcp-port    = 9300
//    es_config-bootstrap-memory_lock = "True"
//    log_volume_mount_full_path      = "/logs"
//    data_volume_mount_full_path     = "/data"
//    nginx_http_template_listen      = 39200
//    nginx_http_client_max_body_size = "16M"
//    nginx_ssl_enabled               = "True"
//    nginx_ssl_acl_enabled           = "True"
//    ssl_trusted_users               = ""
//    ssl_trusted_apps                = "svc-dataproc,svc-k8s"
//    secrets_key_project_id          = "quantexa-perimetered-kms"
//  }
}

