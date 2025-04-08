FROM openjdk:17-jdk-slim

ENV TZ=Asia/Ho_Chi_Minh

RUN apt-get update && \
    apt-get install -y tzdata libc6-locales && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

WORKDIR /app

COPY target/vietchefs-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
