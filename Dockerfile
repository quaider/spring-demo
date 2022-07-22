#FROM bladex/alpine-java:openjdk8-openj9_cn_slim
FROM openjdk:8-jre-alpine
MAINTAINER Kratos Zhang <pto.kratos@hotmail.com>

WORKDIR /app

# Add the service itself
EXPOSE 8080

ARG JAR_FILE
ADD target/${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]