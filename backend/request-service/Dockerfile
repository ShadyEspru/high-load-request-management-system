FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S -g 10001 hlrms \
    && adduser -S -D -H -u 10001 -G hlrms hlrms

COPY --chown=10001:10001 \
    target/request-service-0.0.1-SNAPSHOT.jar \
    app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]