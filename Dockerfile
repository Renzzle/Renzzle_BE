# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk

# mysqldump 명령어를 사용
RUN apt-get update && apt-get install -y default-mysql-client && rm -rf /var/lib/apt/lists/*
# Set the working directory in the container
WORKDIR /app
RUN mkdir -p /app/logs

# Copy the build artifact from the host to the container
COPY build/libs/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java","-jar","/app/app.jar"]