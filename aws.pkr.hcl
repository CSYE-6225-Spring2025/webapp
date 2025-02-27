# Declare variables at the top
variable "aws_region" {
  type    = string
  default = "us-east-1"
}
variable "instance_type" {
  type    = string
  default = "t2.micro"
}
variable "accounts" {
  type    = list(string)
  default = ["123456789012"]
}
variable "ami_name_prefix" {
  type    = string
  default = "my-custom-ami"
}
variable "user_name" {
  type    = string
  default = "csye6225"
}
variable "group_name" {
  type    = string
  default = "csye6225"
}
variable "ssh_username" {
  type    = string
  default = "ubuntu"
}
variable "PACKER_DB_USERNAME" {
  type    = string
  default = "default_db_user"
}

variable "PACKER_DB_PASSWORD" {
  type    = string
  default = "default_db_password"
}

variable "gcp_project_id" {
  default = "development-452002"
}

variable "gcp_zone" {
  default = "us-east1-d"
}

variable "credentials_file" {
  default = "development-creds.json"
}
variable "gcp_demo_account" {
  default = "charged-state-452206-s2"
}
variable "gcp_image_name" {
  default = "packer-gcp-1"
}

packer {
  required_plugins {
    amazon = {
      version = ">=1.2.8"
      source  = "github.com/hashicorp/amazon"
    }
    googlecompute = {
      version = ">=0.2.0"
      source  = "github.com/hashicorp/googlecompute"
    }
  }
}

source "amazon-ebs" "ubuntu" {
  ami_name      = "packer-aws-${formatdate("YYYYMMDDHHmmss", timestamp())}"
  source_ami    = "ami-04b4f1a9cf54c11d0"
  instance_type = var.instance_type
  region        = var.aws_region
  ssh_username  = var.ssh_username
  ami_users     = var.accounts
}

source "googlecompute" "ubuntu" {
  project_id              = var.gcp_project_id
  source_image            = "ubuntu-2404-noble-amd64-v20250214"
  source_image_family     = "ubuntu-2404-noble-amd64"
  credentials_file        = var.credentials_file
  zone                    = var.gcp_zone
  machine_type            = "n1-standard-1"
  disk_size               = 10
  disk_type               = "pd-standard"
  network                 = "default"
  tags                    = ["csye6225"]
  image_project_id        = var.gcp_project_id
  image_description       = "Custom Ubuntu 20.04 server image"
  image_storage_locations = ["us"]
  image_name              = var.gcp_image_name
  image_family            = "my-custom-ami"
  ssh_username            = var.ssh_username
}

build {
  name = "learn-packer"
  sources = [
    "source.amazon-ebs.ubuntu",
    "source.googlecompute.ubuntu"
  ]

  provisioner "shell" {
    inline = [
      "sudo groupadd -f ${var.group_name}",
      "sudo useradd -g ${var.group_name} -M -s /usr/sbin/nologin ${var.user_name}"
    ]
  }


  provisioner "shell" {
    inline = [
      "sudo apt-get update -y || sudo update -y",
      "sudo apt-get install -y openjdk-17-jdk-headless",
    ]
  }

  provisioner "shell" {
    inline = [
      "sudo mkdir -p /etc/myapp",
      "echo 'DB_USERNAME=${var.PACKER_DB_USERNAME}' | sudo tee /etc/myapp/myapp.env",
      "echo 'DB_PASSWORD=${var.PACKER_DB_PASSWORD}' | sudo tee -a /etc/myapp/myapp.env",
      "echo 'SPRING_DATASOURCE_USERNAME=${var.PACKER_DB_USERNAME}' | sudo tee -a /etc/myapp/myapp.env",
      "echo 'SPRING_DATASOURCE_PASSWORD=${var.PACKER_DB_PASSWORD}' | sudo tee -a /etc/myapp/myapp.env",
      "sudo chmod 600 /etc/myapp/myapp.env"
    ]
  }


  provisioner "shell" {
    inline = [

      "sudo apt-get update -y",
      "sudo apt-get install -y mysql-server",
      "sudo systemctl enable mysql",
      "sudo systemctl start mysql",


      "sudo mysql -e \"ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${var.PACKER_DB_PASSWORD}';\"",
      "sudo systemctl restart mysql",
      "echo \"CREATE DATABASE health_check;\" | mysql -u root -p'${var.PACKER_DB_PASSWORD}'",
      "echo \"DROP USER IF EXISTS '${var.PACKER_DB_USERNAME}'@'localhost';\" | mysql -u root -p'${var.PACKER_DB_PASSWORD}'",
      "echo \"CREATE USER '${var.PACKER_DB_USERNAME}'@'localhost' IDENTIFIED BY '${var.PACKER_DB_PASSWORD}';\" | mysql -u root -p'${var.PACKER_DB_PASSWORD}'",
      "echo \"GRANT ALL PRIVILEGES ON health_check.* TO '${var.PACKER_DB_USERNAME}'@'localhost';\" | mysql -u root -p'${var.PACKER_DB_PASSWORD}'",
      "echo \"FLUSH PRIVILEGES;\" | mysql -u root -p'${var.PACKER_DB_PASSWORD}'"
    ]
  }


  provisioner "file" {
    source      = "target/CloudDemo-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/myapp.jar"
  }
  provisioner "shell" {
    inline = [
      "sudo mkdir -p /opt/myapp",
      "sudo mv /tmp/myapp.jar /opt/myapp/myapp.jar",
      "sudo chown -R ${var.user_name}:${var.group_name} /opt/myapp"
    ]
  }

  provisioner "file" {
    source      = "myapp.service"
    destination = "/tmp/myapp.service"
  }

  provisioner "shell" {
    inline = [
      "sudo mv /tmp/myapp.service /etc/systemd/system/myapp.service",
      "sudo chown root:root /etc/systemd/system/myapp.service",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service"
    ]
  }

  post-processor "shell-local" {
    inline = [
      "gcloud compute images add-iam-policy-binding ${var.gcp_image_name} --project=${var.gcp_project_id} --member='project:${var.gcp_demo_account}' --role='roles/compute.imageUser'"
    ]
  }
}