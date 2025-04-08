FROM openjdk:17-slim
ENV TZ=Asia/Ho_Chi_Minh
WORKDIR /app
COPY target/vietchefs-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
