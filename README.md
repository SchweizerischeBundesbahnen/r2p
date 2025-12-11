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

## Build

### Standard Build

Build the application without native app-image:
```bash
mvn clean package
```

This will generate:
* A runnable JAR under `target/r2p-0.0.2-SNAPSHOT.jar`
* A manual and API documentation under `target/docs/`

### Build with Native App-Image

Build the application with platform-specific native app-image:
```bash
mvn clean package -Papp-image
```

This will generate everything from the standard build plus:
* A native application image under `target/dist/r2p/`

The app-image will be automatically created for your platform:
* **Windows**: Includes console window support
* **Linux**: Standard Unix application image
* **macOS**: Standard macOS application bundle

## Structure

* docs:documentation (Asciidoctor)
* src: source code of the prototype