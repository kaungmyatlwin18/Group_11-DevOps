FROM eclipse-temurin:25

WORKDIR /app

COPY target/DevOps_Group_11-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]