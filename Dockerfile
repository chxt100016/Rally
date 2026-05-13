FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY settings.xml pom.xml /app/
COPY rally-adapter /app/rally-adapter
COPY rally-app /app/rally-app
COPY rally-domain /app/rally-domain
COPY rally-infrastructure /app/rally-infrastructure
COPY start /app/start

RUN mvn -s /app/settings.xml -f /app/pom.xml clean package -DskipTests

FROM alpine:3.18

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tencent.com/g' /etc/apk/repositories \
    && apk add --update --no-cache openjdk17-jre-headless tzdata ca-certificates \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && rm -f /var/cache/apk/*

WORKDIR /app

COPY --from=build /app/start/target/start-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 9482

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=wechat"]
