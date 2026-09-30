FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/product-availability-service-*.jar app.jar
USER app

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=cloud
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
