# RU - RU - Partner Communication (R<sup>2</sup>P)

[![CI](https://github.com/SchweizerischeBundesbahnen/r2p/actions/workflows/build.yml/badge.svg)](https://github.com/SchweizerischeBundesbahnen/r2p/actions/workflows/build.yml)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

R<sup>2</sup>P defines a standardized communication framework for information exchange between Railway Undertakings (RUs) and their partners. It enables collaborative planning, operational coordination, disruption management, and settlement processes across organizational boundaries.

The functionality covers:

- Exchange of train and service information
- Coordination of timetable paths and resources
- Synchronization of planning changes
- Operational communication during disruptions
- Sharing of actual, forecast, and settlement data
- Common data model, processes, governance, and security rules

<br>

R<sup>2</sup>P is an encompassing digital communication
- for service, train and resource harmonization among RUs and their partners.
- It is open for RUs and their partners to participate both during the setup and the operation!

It is not 
- a replacement of any path ordering tools, like the national PM systems and PCS for ordering international paths.
- a solution for digitalization of IM harmonization
<br>
<br>
<br>

R<sup>2</sup>P provides a common business data model that establishes a shared understanding of railway services, train operations, resources, and operational events. The model is defined through a Business Object Model (BOM), structured attributes, and JSON representations, ensuring consistent data exchange between systems and organizations.

The data model contains the following elements
- Identifiers
- Train
  - Trip points
  - Resources
    - Formations
    - Personnel
  - Services



<br>
<br>

# Specification

The specification comprises 
- specification
- OpenAPI definition (YAML)
- Data model
- Sample data


## License

This specification is licensed under [CC BY-ND 4.0](https://creativecommons.org/licenses/by-nd/4.0/)



# Reference implementation

## Disclaimer
**This is a PoC implementation not suitable to run in production.**

## Overview

See also the companion webclient that you find here: https://github.com/SchweizerischeBundesbahnen/r2p-webclient

R<sup>2</sup>P is a Spring Boot 4.0 prototype demonstrating railway (RU - Railway Undertaking) partner communication. The prototype uses:
- **H2 embedded database** (no need for a dedicated database server)
- **OpenAPI-first approach** with code generation
- **OAuth2/JWT authentication** via Azure AD
- **Flyway** for database migrations
- **Spring Data JDBC** for data access

## Getting-Started

### Prerequisites

- Java 25 or higher
- Maven 3.6+
- (Optional) Node.js/npm for commit message validation
- (Optional) Docker for containerized deployment

### Build and Test
```bash
mvn clean verify
```
This runs the complete build including code generation, tests, and documentation.

### Run the Application Locally (without Azure)

The easiest way to run the application locally is using the built-in Keycloak setup via Docker Compose.

**Prerequisites:** Docker (or Podman with the Docker CLI wrapper) must be running.

No configuration file needed – the `local` profile is included in `application.yaml`:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Spring Boot Docker Compose support automatically starts the Keycloak container defined in `compose.yaml`. Spring Boot waits for Keycloak to become healthy before the application starts.

**Pre-configured test user:**

| Username   | Password   | Role       |
|------------|------------|------------|
| `r2p-user` | `password` | `r2p_user` |

Access the application:
- **Application**: http://localhost:8080/
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Keycloak Admin Console**: http://localhost:8180/ (admin / admin)
- **Documentation**: `target/docs/`
  - `r2p-manual.html` / `r2p-manual.pdf` - User manual
  - `r2p-api.html` / `r2p-api.pdf` - API documentation

### Run the Application with Azure AD

Create the file `src/main/resources/application-localazure.yaml` (it is git-ignored) with your Azure AD credentials:

```yaml
r2p:
  azure:
    client-id: <your-azure-client-id>
    issuer-uri: <your-azure-issuer-uri>
  instance-owner: <your-organization>
```

The client secret must be provided via the environment variable `R2P_AZURE_CLIENT_SECRET` (not in the config file):

```bash
set R2P_AZURE_CLIENT_SECRET=<your-azure-client-secret>
```

Then start the application:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=localazure"
```

### Run the Application with Docker

Run the latest release:
```bash
docker run -it -p 8080:8080 ghcr.io/schweizerischebundesbahnen/r2p:latest
```

### Database Configuration

By default, the prototype uses an H2 in-memory database (data is lost on restart). To persist data to a file, set the environment variable:
```bash
DATABASE_STORAGE=file:C:/devsbb/h2/r2p
```

## Development

### Design decisions

See Architecture Decision Records (ADRs) in [docs/adr/](docs/adr/) for detailed explanations of key design choices.

### REST endpoints

See [R2P Server REST Endpoints](https://github.com/SchweizerischeBundesbahnen/r2p/wiki/R2P-Server-REST-Endpoints)

### Domain model

See [R2P Server Domain Model](https://github.com/SchweizerischeBundesbahnen/r2p/wiki/R2P-Server-Domain-Model)

### Release Process
- Releases are automated via [release-please](https://github.com/googleapis/release-please-action)
- Merging to `main` triggers release creation based on commit messages
- Each release includes:
  - Docker image (`ghcr.io/schweizerischebundesbahnen/r2p`)
  - Native executables for Linux, Windows, and macOS
  - Documentation (HTML + PDF)

See [RELEASE.md](RELEASE.md) for more details.

## Deployment

Deploy to OpenShift - see [docs/manual.adoc](docs/manual.adoc) for details.

## Project Structure

* `docs/` - Documentation (AsciiDoctor)
* `src/main/java/` - Source code of the prototype
* `src/main/resources/openapi/` - OpenAPI specification (API contract)
* `src/main/resources/db/migration/` - Flyway database migrations
* `src/test/java/` - Unit and integration tests
* `.github/workflows/` - CI/CD pipelines

## Documentation

Please consider the following information:

- [CODING_STANDARDS.md](CODING_STANDARDS.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)


## License

This project is licensed under [Apache 2.0](LICENSE).

## Contributing

Open-source projects thrive on collaboration and contributions from the community. All
contributors are requested to read the [CONTRIBUTING.md](CONTRIBUTING.md) file as it outlines how to
contribute to the project, including how to submit bug reports, feature requests, and pull requests.

## Coding Standards

All contributors are requested to read the [CODING_STANDARDS.md](CODING_STANDARDS.md) file that
outlines the coding standards that you should follow when contributing to the project.

## Code of Conduct

To ensure that the project is a welcoming and inclusive environment for all contributors, it has established a [Code of Conduct](CODE_OF_CONDUCT.md).

