FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/scoring-service-0.1.0.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
