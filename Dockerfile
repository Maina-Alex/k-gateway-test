FROM adoptopenjdk/openjdk11:alpine-slim
VOLUME /opt
EXPOSE 9700
COPY api-gateway.jar app.jar
COPY application.yml application.yml
COPY application-prod.yml application-prod.yml
ENTRYPOINT ["java","-jar","app.jar"]