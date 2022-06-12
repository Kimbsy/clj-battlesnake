# Use https://hub.docker.com/_/oracle-serverjre-8
FROM java:8-alpine

# Make a directory
RUN mkdir -p /app
WORKDIR /app

# Copy only the target jar over
COPY target/battlesnake-1.0.0-standalone.jar .

# Open the port
EXPOSE 80

# Run the JAR
CMD java -jar battlesnake-1.0.0-standalone.jar
