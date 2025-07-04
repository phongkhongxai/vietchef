FROM openjdk:17-jdk-slim

ENV TZ=Asia/Ho_Chi_Minh

RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata

WORKDIR /app

COPY target/vietchefs-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources src/main/resources

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
