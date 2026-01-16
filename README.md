# R2P

Prototype for RU - RU - Partner communication

## Prerequisites

- Java 25 or higher
- Maven 3.6+

## Quickstart

Test and execute the application:
```bash
mvn clean verify
mvn spring-boot:run
```

or

```bash
docker run -it -p 8080:8080 ghcr.io/schweizerischebundesbahnen/r2p:latest
```

Access the application under http://localhost:8080/, you will find the documentation under
`target/docs/`

## Deployment

Deploy to OpenShift - see `docs/manual.adoc` for details.

## Structure

* docs:documentation (Asciidoctor)
* src: source code of the prototype