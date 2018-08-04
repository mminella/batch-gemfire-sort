FROM openjdk:8-jdk-alpine
VOLUME /tmp
MAINTAINER Michael T Minella <mminella@pivotal.io>
ADD target/batch-gemfire-file-sort-master-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT exec java -jar app.jar