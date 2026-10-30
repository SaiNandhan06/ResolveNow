FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/eurekaServer-0.0.1-SNAPSHOT.war app.war
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.war"]
