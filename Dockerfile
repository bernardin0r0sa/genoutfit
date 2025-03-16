# Use a Maven image to build the application
FROM maven:3.8-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and the source code
COPY pom.xml /app/
COPY src /app/src

# If you have a package.json file, copy it into the container
COPY package.json /app/
#COPY package-lock.json /app/

# Build the application
RUN mvn clean package -DskipTests

# Use an OpenJDK image to run the application
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built jar file from the Maven container
COPY --from=build /app/target/genoutfit-1.0.0.jar ./genoutfit.jar

# Copy the static resources
COPY --from=build /app/src/main/resources/static ./src/main/resources/static

# Create a directory for logs
RUN mkdir -p /app/logs

# Install necessary packages
RUN apt-get update && apt-get install -y \
    openssl \
    ca-certificates

# Download the server's certificate
RUN apt-get update && apt-get install -y openssl
RUN openssl s_client -showcerts -connect c2137757a033f71e5abddb52bba08b3f.r2.cloudflarestorage.com:443 </dev/null 2>/dev/null | openssl x509 -outform PEM > /app/server.crt

# Import the certificate into the Java trust store
RUN keytool -import -alias cloudflare-r2 -file /app/server.crt -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

# Expose the port your application runs on
EXPOSE 8080

# Run the application with logging enabled
ENTRYPOINT ["java", "-Dlogging.file=/app/logs/application.log", "-jar", "genoutfit.jar"]