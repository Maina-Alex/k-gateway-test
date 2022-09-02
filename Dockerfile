FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine

VOLUME /opt

EXPOSE 9700

COPY api-gateway.jar app.jar
COPY application.yml application.yml
COPY application-dev.yml application-dev.yml

ENTRYPOINT ["java","-jar","app.jar"]