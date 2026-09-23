FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/signup-service-0.1.0.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
