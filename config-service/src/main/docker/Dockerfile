FROM openjdk:8-jre-alpine

ARG GIT_COMMIT="git hash not set"
ENV GIT_COMMIT ${GIT_COMMIT}

ADD *.jar /app.jar

RUN apk --no-cache add curl
RUN apk --no-cache add bash

COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY config-yml-files /config-yml-files

RUN ["chmod", "+x", "/docker-entrypoint.sh"]
RUN ["dos2unix", "/docker-entrypoint.sh"]

EXPOSE 8888

ENTRYPOINT ["/docker-entrypoint.sh"]