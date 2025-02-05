#!/bin/bash


DB_NAME="health_check"
DB_USERNAME="$DB_USERNAME"
DB_PASSWORD="$DB_PASSWORD"
GROUP_NAME="csye6225"
USER_NAME="student"
APP_ZIP="PraveenKumar_VijayaKumar_002320648_02.zip"


# Update the list of packages
echo "Updating package lists"
sudo apt-get update -y

# Upgrade the packages in the system
echo "Upgrading packages"
sudo apt-get upgrade -y

# install Mysql server in server
echo "Installing MySQL"
sudo apt-get install mysql-server -y

echo "Database installation done"


# Securing Mysql 
sudo mysql_secure_installation <<EOF

n  # No root Password
y  # Delete anonymous users
y  # Should not allow root login remotely
y  # Remove test database
y  # Reload privilege tables
EOF
echo "Database Secure Done"

# Creating the Database
echo "Creating Database"


mysql -u root -e "CREATE DATABASE ${DB_NAME};"
mysql -u root -e "CREATE USER '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';"
mysql -u root -e "GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost';"
mysql -u root -e "FLUSH PRIVILEGES;"


# Create a new Linux group for the application
echo "Creating new Linux group"
sudo groupadd ${GROUP_NAME}

# Create a new user for the application
echo "Creating new Linux user"
sudo useradd -m -s /bin/bash -g ${GROUP_NAME} ${USER_NAME}


#Install unzip application
sudo apt-get install zip unzip -y

echo "Unzipping application"
sudo mkdir -p /opt/csye6225
sudo unzip ${APP_ZIP} -d /opt/csye6225

echo "Updating permissions"
sudo chown -R ${USER_NAME}:${GROUP_NAME} /opt/csye6225
sudo chmod -R 750 /opt/csye6225

export SPRING_DATASOURCE_USERNAME=$DB_USERNAME
export SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD


echo "Installing Maven dependency"
apt install maven -y

echo "Installing Java 17"
sudo apt install openjdk-17-jdk -y

# Set Java 17 as default
echo "Setting Java 17 as default Java version"
sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac

# Verify the default version
java -version
cd PraveenKumar_VijayaKumar_002320648_02
cd webapp

echo "Building Started for the Web application"
mvn clean install

echo "Starting the Web application"
mvn spring-boot:run