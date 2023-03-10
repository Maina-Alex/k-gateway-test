FROM adoptopenjdk/openjdk11:alpine
WORKDIR /app
EXPOSE 7600
COPY target/*.jar app.jar
COPY application.yml application.yml
COPY application-uat.yml application-uat.yml
ENTRYPOINT java -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -Djava.awt.headless=true -XX:+UseContainerSupport -jar app.jar