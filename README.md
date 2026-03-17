# R2P

Prototype for RU - RU - Partner communication

[![Build and Test](https://github.com/SchweizerischeBundesbahnen/r2p/actions/workflows/build.yml/badge.svg)](https://github.com/SchweizerischeBundesbahnen/r2p/actions/workflows/build.yml)
[![Lint Commit Messages](https://github.com/SchweizerischeBundesbahnen/r2p/actions/workflows/lint-commit-messages.yml/badge.svg)](https://github.com/SchweizerischeBundesbahnen/r2p/actions/workflows/lint-commit-messages.yml)

## Overview

R2P is a Spring Boot 4.0 prototype demonstrating railway (RU - Railway Undertaking) partner communication. The prototype uses:
- **H2 embedded database** (no need for a dedicated database server)
- **OpenAPI-first approach** with code generation
- **OAuth2/JWT authentication** via Azure AD
- **Flyway** for database migrations
- **Spring Data JDBC** for data access

## Prerequisites

- Java 25 or higher
- Maven 3.6+
- (Optional) Node.js/npm for commit message validation

## Quickstart

### Build and Test
```bash
mvn clean verify
```
This runs the complete build including code generation, tests, and documentation.

### Run the Application

Define the environment variable `R2P_AZURE_CLIENT_SECRET` (and `R2P_AZURE_CLIENT_ID` if necessary) for Azure AD authentication.

Then start the application:

```bash
mvn spring-boot:run
```

Access the application:
- **Application**: http://localhost:8080/
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Documentation**: `target/docs/` (HTML + PDF)

**Note:** To access secured endpoints, you must be a member of AD group `DG_SEC_DEV_R2P_Admin`.

### Docker
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

### Commit Messages
All commits **must** follow [Conventional Commits](https://www.conventionalcommits.org/) format:
```
type(scope): description

Examples:
- feat(api): add new train endpoint
- fix(persistence): correct null handling
- docs: update README
```

To enable automatic validation, run:
```bash
npm install
```

### Release Process
- Releases are automated via [release-please](https://github.com/googleapis/release-please-action)
- Merging to `main` triggers release creation based on commit messages
- Each release includes:
  - Docker image (`ghcr.io/schweizerischebundesbahnen/r2p`)
  - Native executables for Linux, Windows, and macOS
  - Documentation (HTML + PDF)

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

## Deployment

Deploy to OpenShift - see `docs/manual.adoc` for details.

## Project Structure

* `docs/` - Documentation (AsciiDoctor)
* `src/main/java/` - Source code of the prototype
* `src/main/resources/openapi/` - OpenAPI specification (API contract)
* `src/main/resources/db/migration/` - Flyway database migrations
* `src/test/java/` - Unit and integration tests
* `.github/workflows/` - CI/CD pipelines

## License

See [gpl-3.0.txt](gpl-3.0.txt)

