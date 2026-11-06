FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/distributed-job-processor-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
