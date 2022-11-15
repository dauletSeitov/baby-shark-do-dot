FROM openjdk:11
COPY build/libs/baby-shark-do-dot-0.0.1-SNAPSHOT.jar  /usr/src/app.jar
ENTRYPOINT ["java", "-jar", "/usr/src/app.jar"]