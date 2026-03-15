# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y default-mysql-client g++ && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /app/logs

# C++ AI 엔진 컴파일
COPY src/main/cpp/ /app/cpp/
RUN g++ -O3 -std=c++17 -I/app/cpp -o /usr/local/bin/renzzle_ai_engine /app/cpp/main.cpp \
    && rm -rf /app/cpp

# Copy the build artifact from the host to the container
COPY build/libs/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java","-jar","/app/app.jar"]