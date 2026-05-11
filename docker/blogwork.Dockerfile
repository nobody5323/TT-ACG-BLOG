FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY framework/pom.xml framework/pom.xml
COPY framework/src framework/src
COPY blogwork/pom.xml blogwork/pom.xml
COPY blogwork/src blogwork/src

RUN mvn -f framework/pom.xml clean install -DskipTests
RUN mvn -f blogwork/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/blogwork/target/*.jar /app/app.jar

EXPOSE 7979

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.config.location=file:/app/config/application.yml"]

