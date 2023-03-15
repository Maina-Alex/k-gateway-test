mvn clean package -DskipTests
docker build -t devops-registry.ekenya.co.ke/channel-manager/channel-gateway:latest .
docker push devops-registry.ekenya.co.ke/channel-manager/channel-gateway:latest