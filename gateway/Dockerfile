FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/gateway-0.1.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
