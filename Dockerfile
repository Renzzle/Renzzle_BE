# Use a JRE runtime image; the application is already built before docker build.
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y default-mysql-client && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /app/logs

# Copy the executable Spring Boot artifact from the host to the container.
COPY build/libs/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java","-jar","/app/app.jar"]
