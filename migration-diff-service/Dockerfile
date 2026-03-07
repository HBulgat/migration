FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY target/migration-diff-service-*.jar app.jar

ENTRYPOINT ["java", "-Xmx512m", "-Xms512m", "-jar", "app.jar"]
