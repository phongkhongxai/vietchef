FROM openjdk:17-jdk-slim
ENV TZ=Asia/Ho_Chi_Minh
WORKDIR /app
COPY target/vietchefs-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
