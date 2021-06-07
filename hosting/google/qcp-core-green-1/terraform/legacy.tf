#These are legacy module used for shared dataproc

/*
module "dataproc" {
  source      = "git::ssh://git@gitlab.com/Quantexa/dev-ops/terraform-modules/Google-Dataproc.git?ref=0.1.5"
  projectName = "${data.terraform_remote_state.project-core.project_id}"
  subnet      = "${data.terraform_remote_state.project-core.dpr_subnet_self_link}"
  region      = "${data.terraform_remote_state.project-core.region}"
  zone        = "${data.terraform_remote_state.project-core.zone}"

  serviceAccount = "${data.terraform_remote_state.project-core.dataproc_svc_account}"
  sshUsername    = "${var.ssh_username}"
  sshPublicKey   = "${var.ssh_authorized_keys}"

  //Override
  clusterOn                  = "${var.legacy-project-config-map["dataproc-on"]}"
  masterNodeBootDiskGB       = 20
  masterNodeType             = "${var.legacy-project-config-map["dataproc-master-node-type"]}"
  workerNodeCount            = "${var.legacy-project-config-map["dataproc-worker-node-count"]}"
  workerNodeType             = "${var.legacy-project-config-map["dataproc-worker-node-type"]}"
  workerNodeNumLocal375GBSSD = "${var.legacy-project-config-map["dataproc-worker-local-ssds"]}"
  blockProjectSshKeys        = true

  preemptibleWorkerNodeCount = "${var.legacy-project-config-map["dataproc-pre-emptible-worker-node-count"]}"
}
*/
