FROM openjdk:8-jre-alpine

ARG GIT_COMMIT="git hash not set"
ENV GIT_COMMIT ${GIT_COMMIT}

ADD *.jar /app.jar

RUN apk --no-cache add curl
RUN apk --no-cache add bash

COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY config-files /config-files
COPY quantexa-licence /quantexa-licence

RUN ["chmod", "+x", "/docker-entrypoint.sh"]
RUN ["dos2unix", "/docker-entrypoint.sh"]

EXPOSE 8421 2555

ENTRYPOINT ["/docker-entrypoint.sh"]
