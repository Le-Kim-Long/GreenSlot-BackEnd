# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/GreeenSlot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms200m", "-Xmx350m", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
# Create the expected folder inside the container
RUN mkdir -p firebase

# Create a symbolic link pointing to Render's mounted secret file
RUN ln -s /etc/secrets/greenslot-46382-firebase-adminsdk-fbsvc-5a99ada1a5.json firebase/greenslot-46382-firebase-adminsdk-fbsvc-5a99ada1a5.json
