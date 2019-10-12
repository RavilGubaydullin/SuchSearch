FROM java:openjdk-8-jre
LABEL maintainer=ravil
WORKDIR /app
COPY libs libs/
COPY classes classes/
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-cp", "/app/resources:/app/classes:/app/libs/*", "main.Application"]
