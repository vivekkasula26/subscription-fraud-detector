FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/case-service-0.1.0.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
