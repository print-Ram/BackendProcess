FROM openjdk:17-jdk-slim

# Copy the jar built by Maven into the image
COPY target/home-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]
