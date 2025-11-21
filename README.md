# R2P

Prototype for RU - RU - Partner communication

## Prerequisites

- Java 25 or higher
- Maven 3.6+

## Package & Run

```bash
# Package (includes docs generation)
mvn package

# Run
mvn spring-boot:run
```

Access the application under http://localhost:8080/

You will find the generated documentation under `target/docs/`.

## Structure

* docs: Generated documentation (Asciidoctor)
* src: Source code of the prototype