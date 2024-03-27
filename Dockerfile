FROM alpine:latest AS build
WORKDIR /app
COPY . .
RUN apk add openjdk17
RUN /bin/sh gradlew shadowJar

FROM alpine:latest
LABEL authors="vasiliy.p.motchenko@gmail.com"
WORKDIR /app
COPY --from=build /app/build/libs/vpn-bot-0.1-all.jar /app/vpn-bot.jar
COPY src/sql /sql
RUN apk add --no-cache openjdk17 openssl
RUN mkdir /clients
ENTRYPOINT ["java", "-jar", "/app/vpn-bot.jar"]