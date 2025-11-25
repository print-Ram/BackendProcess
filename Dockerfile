FROM eclipse-temurin:17-jre-jammy
COPY home-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "/app.jar"]
