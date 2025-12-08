# Use a base image with a Java Runtime Environment (JRE)
FROM amazoncorretto:21-alpine-jdk

# Setting the working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY target/transaction-aggregator-0.0.1-SNAPSHOT.jar data-aggregation-app.jar

# Expose the port
EXPOSE 8081

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "data-aggregation-app.jar"]
