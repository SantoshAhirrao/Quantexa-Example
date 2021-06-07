FROM openjdk:8-jre-alpine

ARG GIT_COMMIT="git hash not set"
ENV GIT_COMMIT ${GIT_COMMIT}

ADD *.jar /app.jar

RUN apk --no-cache add curl
RUN apk --no-cache add bash

RUN mkdir -p /etc/ssl/quantexa/

# needed for SAML, remove if only LDAP needed for auth
COPY quantexa.p12 /etc/ssl/quantexa/quantexa.p12
COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN ["chmod", "+x", "/docker-entrypoint.sh"]
RUN ["dos2unix", "/docker-entrypoint.sh"]

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
