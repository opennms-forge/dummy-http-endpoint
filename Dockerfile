FROM openjdk:8-jdk-alpine
VOLUME /tmp
RUN apk add --no-cache curl
ARG JAR_FILE
ADD ${JAR_FILE} /opt/app.jar
HEALTHCHECK --interval=10s --timeout=3s CMD curl --fail --silent --head http://localhost:8080 | grep "HTTP/1.1 200" || exit 1
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/app.jar"]