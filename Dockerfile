# Build stage using official Maven & JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom and resolve dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source and build
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Run stage using lightweight JRE 21 Alpine
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/skillsync-0.0.1-SNAPSHOT.war app.war

EXPOSE 8081
ENTRYPOINT ["java", "-Xmx350m", "-jar", "app.war"]

