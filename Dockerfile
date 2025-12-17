FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY src src
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
ENV TZ=Europe/Zurich
RUN ./mvnw clean package -DskipTests
RUN mv target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:25-jre
RUN addgroup spring && useradd -g spring spring
USER spring:spring
WORKDIR /app
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/application/ ./
ENV TZ=Europe/Zurich
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

