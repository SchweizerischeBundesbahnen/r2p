# R2P

Prototype for RU - RU - Partner communication

## Technologies

- Spring Boot 4
- OpenAPI 3.0.3 / Swagger UI
- Flyway (Database Migration)
- Spring Security
- H2 Database

## Prerequisites

- Java 25 or higher
- Maven 3.6+

## Build & Run

```bash
# Compile
mvn clean compile

# Test
mvn test

# Package
mvn package

# Run
mvn spring-boot:run
# or
java -jar target/r2p-0.0.1-SNAPSHOT.jar
```

## Accessing the Application

- **Root**: http://localhost:8080/ → redirects to Swagger UI
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **HelloWorld API**: http://localhost:8080/helloworld