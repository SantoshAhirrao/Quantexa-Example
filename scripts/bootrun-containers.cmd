cd ..

SET SPRING_PROFILES_ACTIVE=development,postgres

start gradlew app-investigate:bootRun

start gradlew app-resolve:bootRun

start gradlew app-search:bootRun

start gradlew app-security:bootRun

start gradlew app-graph-script:bootRun

start gradlew gateway:bootRun
