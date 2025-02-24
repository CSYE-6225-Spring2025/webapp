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
variable "DB_USERNAME" {
  type    = string
  default = "default_db_user"
}

variable "DB_PASSWORD" {
  type    = string
  default = "default_db_password"
}

packer {
  required_plugins {
    amazon = {
      version = ">=1.2.8"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

source "amazon-ebs" "ubuntu" {
  ami_name      = "learn-packer-linux-aws"
  source_ami    = "ami-04b4f1a9cf54c11d0"
  instance_type = var.instance_type
  region        = var.aws_region
  ssh_username  = var.ssh_username
  ami_users     = var.accounts
}

build {
  name = "learn-packer"
  sources = [
    "source.amazon-ebs.ubuntu"
  ]
  # 1) Create csye6225 user
  provisioner "shell" {
    inline = [
      "sudo groupadd -f ${var.group_name}",
      "sudo useradd -g ${var.group_name} -M -s /usr/sbin/nologin ${var.user_name}"
    ]
  }

  # 2) Install dependencies
  provisioner "shell" {
    inline = [
      "sudo apt-get update -y || sudo update -y",
      "sudo apt-get install -y openjdk-17-jdk-headless",
    ]
  }

  provisioner "shell" {
    inline = [
      "sudo mkdir -p /etc/myapp",
      "echo 'DB_USERNAME=${var.DB_USERNAME}' | sudo tee /etc/myapp/myapp.env",
      "echo 'DB_PASSWORD=${var.DB_PASSWORD}' | sudo tee -a /etc/myapp/myapp.env",
      "echo 'SPRING_DATASOURCE_USERNAME=${var.DB_USERNAME}' | sudo tee -a /etc/myapp/myapp.env",
      "echo 'SPRING_DATASOURCE_PASSWORD=${var.DB_PASSWORD}' | sudo tee -a /etc/myapp/myapp.env",
      "sudo chmod 600 /etc/myapp/myapp.env"
    ]
  }


  provisioner "shell" {
    inline = [
      # 1) Update the package list
      "sudo apt-get update -y",

      # 2) Install MySQL server (not just the client)
      "sudo apt-get install -y mysql-server",

      # 3) Make sure MySQL is started and enabled on boot
      "sudo systemctl enable mysql",
      "sudo systemctl start mysql",

      # Set MySQL root password using variable
      "sudo mysql -e \"ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${var.DB_PASSWORD}';\"",
      "sudo systemctl restart mysql",

      # 4) Create a dedicated database and user inside MySQL
      #    We pipe commands into "sudo mysql" so they run as root without needing a password
      "echo \"CREATE DATABASE health_check;\" | mysql -u root -p'${var.DB_PASSWORD}'",
      "echo \"DROP USER IF EXISTS '${var.DB_USERNAME}'@'localhost';\" | mysql -u root -p'${var.DB_PASSWORD}'",
      "echo \"CREATE USER '${var.DB_USERNAME}'@'localhost' IDENTIFIED BY '${var.DB_PASSWORD}';\" | mysql -u root -p'${var.DB_PASSWORD}'",
      "echo \"GRANT ALL PRIVILEGES ON health_check.* TO '${var.DB_USERNAME}'@'localhost';\" | mysql -u root -p'${var.DB_PASSWORD}'",
      "echo \"FLUSH PRIVILEGES;\" | mysql -u root -p'${var.DB_PASSWORD}'"
    ]
  }

  # 3) Copy the Spring Boot JAR you built locally
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
  # 4) Copy a file with environment variables


  # 5) Move env file into place, set permissions
  # 6) Copy systemd service file
  provisioner "file" {
    source      = "myapp.service"
    destination = "/tmp/myapp.service"
  }

  # 7) Enable the service
  provisioner "shell" {
    inline = [
      "sudo mv /tmp/myapp.service /etc/systemd/system/myapp.service",
      "sudo chown root:root /etc/systemd/system/myapp.service",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service"
    ]
  }
}