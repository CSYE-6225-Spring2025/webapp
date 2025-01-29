
# CSYE 6225 Network Structure & Cloud Computing
# webapp

The Repository contains a health check Api which is used to check the running status of a application.

## Steps to Download and Run the Code:
1. Clone the Repository using SSH.
2. After cloning the Repository in local, open the folder in any IDE.
3. To build the application, give the command ( mvn clean install)
4. Once the build is done go to Run configurations and select Java 17 as java runtime version and add the following as environment variables :
    4.1 SPRING_DATASOURCE_USERNAME = {username of the Database}
    4.2 SPRING_DATASOURCE_PASSWORD = {password for the Database}
5. Now click Apply and then ok.
6. Click run application button to run the application.
