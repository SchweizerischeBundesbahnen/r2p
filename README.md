# R2P

Prototype for RU - RU - Partner communication

## Prerequisites

- Java 25 or higher
- Maven 3.6+

## Quickstart

Execute the application:
```bash
mvn spring-boot:run
```

Access the application under http://localhost:8080/

## Generate a release

```bash
mvn clean package -Prelease
```

This will generate
* A runnable JAR under `target/r2p-0.0.1-SNAPSHOT.jar`
* A manual and API documentation under `target/docs/`
* A native application image under `target/dist/r2p/`

## Structure

* docs:documentation (Asciidoctor)
* src: source code of the prototype