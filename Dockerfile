# Use the requested Red Hat UBI Java 17 runtime image
FROM registry.access.redhat.com/ubi9/openjdk-17

WORKDIR /deployments

# Copy the Spring Boot JAR built by Maven
COPY target/*.jar app.jar

EXPOSE 8080

# Optional JVM tuning
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /deployments/app.jar"]
