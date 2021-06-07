FROM java:openjdk-8-jre-alpine

ARG GIT_COMMIT="git hash not set"
ENV GIT_COMMIT ${GIT_COMMIT}

ADD *.jar /app.jar

RUN apk --no-cache add curl
RUN apk --no-cache add bash

COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN ["chmod", "+x", "/docker-entrypoint.sh"]

EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]