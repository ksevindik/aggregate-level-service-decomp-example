# Use the official Maven image to build the application
FROM maven:3.8.3-openjdk-17 AS build


# Set the working directory in the container
WORKDIR /app

# Copy the Maven project file and download dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy the source code into the container
COPY src ./src

# Build the application
RUN mvn -DskipTests -B package

# Use AdoptOpenJDK image to run the application
FROM openjdk:17-alpine

# Set the working directory
WORKDIR /app

# Copy the packaged JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 8090

# Command to run the application
CMD ["java", "-jar", "app.jar"]