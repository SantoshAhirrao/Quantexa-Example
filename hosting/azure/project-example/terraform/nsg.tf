resource "azurerm_network_security_group" "elastic-data-nsg" {
  count               = "${var.project-config-map["elastic-on"]}"
  name                = "${data.terraform_remote_state.project-core.project-name}-elastic-data-nsg"
  location            = "${data.terraform_remote_state.project-core.region}"
  resource_group_name = "${data.terraform_remote_state.project-core.elk-rg-name}"

  security_rule {
    name                   = "ssh-azurevpn-gcp-aws-azure"
    priority               = 100
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "22"

    source_address_prefixes = [
      "${data.terraform_remote_state.project-core.remote_ips["openvpnclientpool_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["azurevpn_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["gcpcore_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["awsprimary_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["azurecore_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["waterloo_mgt_cidr"]}"
    ]

    destination_address_prefix = "*"
  }

  security_rule {
    name                   = "elastic-nginx-https"
    priority               = 103
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "39200"

    source_address_prefixes = [
      "${data.terraform_remote_state.project-core.elk-subnet-cidr}",
      "${data.terraform_remote_state.project-core.aks-subnet-cidr}",
      "${data.terraform_remote_state.project-core.hdi-subnet-cidr}"
    ]

    destination_address_prefix = "*"
  }

  security_rule {
    name                   = "elastic-transport"
    priority               = 104
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "9300"

    source_address_prefixes = [
      "${data.terraform_remote_state.project-core.elk-subnet-cidr}",
      "${data.terraform_remote_state.project-core.hdi-subnet-cidr}"
    ]

    destination_address_prefix = "*"
  }

  #Allow on hosting services subnet for monitoring
  security_rule {
    name                       = "allow-node-exporter"
    priority                   = 3500
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "9100"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  #Allow on hosting services subnet for monitoring
  security_rule {
    name                       = "allow-elastic-exporter"
    priority                   = 3510
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "9108"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-monitoring-vm-nginx-exporter"
    priority                   = 3520
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "9113"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-monitoring-vm-fluentd-exporter"
    priority                   = 3530
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "24231"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  #Allow on gateway subnet preventing site-to-site connectivity issues
  security_rule {
    name                       = "allow-gateway-subnet"
    priority                   = 3999
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "${data.terraform_remote_state.project-core.remote_ips["vpngateway_cidr"]}"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "deny-all"
    priority                   = 4000
    direction                  = "Inbound"
    access                     = "Deny"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  tags {
    projectName = "${data.terraform_remote_state.project-core.project-name}"
  }
}

resource "azurerm_network_security_group" "elastic-kibana-nsg" {
  count               = "${var.project-config-map["kibana-on"] * var.project-config-map["elastic-on"]}"
  name                = "${data.terraform_remote_state.project-core.project-name}-elastic-kibana-nsg"
  location            = "${data.terraform_remote_state.project-core.region}"
  resource_group_name = "${data.terraform_remote_state.project-core.elk-rg-name}"

  security_rule {
    name                   = "ssh-azurevpn-gcp-aws-azure"
    priority               = 100
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "22"

    source_address_prefixes = [
      "${data.terraform_remote_state.project-core.remote_ips["openvpnclientpool_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["azurevpn_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["gcpcore_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["awsprimary_cidr"]}",
      "${data.terraform_remote_state.project-core.remote_ips["azurecore_cidr"]}",
    ]

    destination_address_prefix = "*"
  }

  security_rule {
    name                   = "kibana"
    priority               = 105
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "5601"

    source_address_prefixes = [
      "${data.terraform_remote_state.project-core.elk-subnet-cidr}",
      "${data.terraform_remote_state.project-core.hdi-subnet-cidr}",
    ]

    destination_address_prefix = "*"
  }

  #Allow on hosting services subnet for monitoring
  security_rule {
    name                       = "allow-node-exporter"
    priority                   = 3500
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "9100"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  #Allow on gateway subnet preventing site-to-site connectivity issues
  security_rule {
    name                       = "allow-gateway-subnet"
    priority                   = 3999
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "${data.terraform_remote_state.project-core.remote_ips["vpngateway_cidr"]}"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "deny-all"
    priority                   = 4000
    direction                  = "Inbound"
    access                     = "Deny"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  tags {
    projectName = "${data.terraform_remote_state.project-core.project-name}"
  }
}
