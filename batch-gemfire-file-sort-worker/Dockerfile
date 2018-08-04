FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Dgemfire.start-locator=localhost[10334]","-jar","/app.jar","--partition.name=0","--spring.data.gemfire.name=SortServerZero","--spring.batch.working-directory=/tmp/sort/partition0"]
