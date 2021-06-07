#!/bin/sh

OTHER_VM_ARGS_UNQUOTED=`echo $OTHER_VM_ARGS | sed 's|\"||g'`

SPRING_PROFILES=docker
if [ ! -z "$OTHER_SPRING_PROFILES_ACTIVE" ]; then
  OTHER_SPRING_PROFILES_ACTIVE_UNQUOTED=`echo $OTHER_SPRING_PROFILES_ACTIVE | sed 's|\"||g'`
  SPRING_PROFILES=`echo $SPRING_PROFILES,$OTHER_SPRING_PROFILES_ACTIVE_UNQUOTED`
fi

if [ ! -z "$KEYSTORE_PASSWORD" ]; then
    KEYSTORE_PASSWORD_PROPERTY="-Djavax.net.ssl.keyStorePassword=$KEYSTORE_PASSWORD"
fi

if [ ! -z "$TRUSTSTORE_PASSWORD" ]; then
    TRUSTSTORE_PASSWORD_PROPERTY="-Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD"
fi

echo "java -Dspring.profiles.active=$SPRING_PROFILES -Djava.security.egd=file:/dev/./urandom $KEYSTORE_PASSWORD_PROPERTY $TRUSTSTORE_PASSWORD_PROPERTY $OTHER_VM_ARGS_UNQUOTED -jar /app.jar"
java -Dspring.profiles.active=$SPRING_PROFILES \
-Djava.security.egd=file:/dev/./urandom \
$KEYSTORE_PASSWORD_PROPERTY \
$TRUSTSTORE_PASSWORD_PROPERTY \
$OTHER_VM_ARGS_UNQUOTED \
-jar /app.jar
