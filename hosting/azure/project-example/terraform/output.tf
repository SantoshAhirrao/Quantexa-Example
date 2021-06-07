output "elk-sto-name" {
  value = "${data.terraform_remote_state.project-core.elk-sto-name}"
}

output "elk-sto-access" {
  value = "${data.terraform_remote_state.project-core.elk-sto-access}"
}

output "aks-log-sto-name" {
  value = "${data.terraform_remote_state.project-core.aks-log-sto-name}"
}

output "aks-log-sto-access" {
  value = "${data.terraform_remote_state.project-core.aks-log-sto-access}"
}

output "postgres-server-name" {
  value = "${data.terraform_remote_state.project-core.postgres-server-name}"
}

output "postgres-password" {
  value = "${data.terraform_remote_state.project-core.postgres-password}"
}