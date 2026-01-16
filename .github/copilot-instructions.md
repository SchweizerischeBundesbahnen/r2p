# Copilot Agent Instructions for R2P Repository

## Project Overview

**R2P** (RU-RU-Partner) is a Spring Boot 4.0 prototype for railway (RU - Railway Undertaking) partner communication. It's a medium-sized Java project using an H2 embedded database, Flyway migrations, OpenAPI code generation, and Azure AD OAuth2 authentication.

**Key Technologies:**
- Java 25, Maven 3.6+, Spring Boot 4.0.1
- H2 Database (in-memory or file-based), Spring Data JDBC, Flyway
- OpenAPI Generator (spring server & asciidoc documentation)
- Security: OAuth2/JWT (Azure AD), Spring Security
- Lombok, Jackson, SpringDoc OpenAPI UI

## Build Commands (ALWAYS use these)

### Primary Build & Test
```bash
mvn clean verify
```
This runs the full build including:
1. OpenAPI code generation (generates DTOs and API interfaces)
2. Compilation
3. Unit tests
4. AsciiDoc documentation generation (HTML + PDF)
5. JAR packaging

**Build time:** ~30-40 seconds

### Run Application Locally
```bash
mvn spring-boot:run
```
Access at: http://localhost:8080/

### Quick Compile (no tests)
```bash
mvn clean package -DskipTests
```

### Platform-Specific Native Images
- Windows: `mvn clean package -Papp-image-windows`
- Linux/Unix: `mvn clean package -Papp-image-unix`
- macOS: `mvn clean package -Papp-image-mac`

## Project Structure

```
r2p/
├── pom.xml                           # Maven build configuration
├── src/main/java/ch/rail/r2p/
│   ├── R2pApplication.java           # Main entry point
│   ├── Constants.java                # Shared constants
│   ├── config/                       # Spring configuration classes
│   │   ├── SecurityConfig.java       # OAuth2/JWT security setup
│   │   ├── JacksonConfig.java        # JSON serialization config
│   │   └── OpenApiSecurityConfig.java
│   ├── domain/                       # Domain models (TrainVersion, Changelog)
│   ├── persistence/                  # Data access layer
│   │   └── impl/                     # Repository implementations
│   └── web/                          # REST controllers and services
│       ├── TrainController.java      # Main API controller (implements V1Api)
│       ├── TrainService.java         # Business logic
│       └── TrainMapper.java          # DTO mapping
├── src/main/resources/
│   ├── application.yaml              # Spring Boot configuration
│   ├── db/migration/V1__create.sql   # Flyway DB schema
│   └── openapi/
│       ├── r2p-spec.yaml             # OpenAPI specification (SOURCE OF TRUTH)
│       └── schemas/train.yaml        # Train data schema
├── src/test/java/ch/rail/r2p/
│   ├── web/                          # Controller tests
│   └── WithMockR2PUser.java          # Test annotation for security
├── target/generated-sources/openapi/ # GENERATED - do not edit manually
│   └── src/main/java/ch/rail/r2p/api/
│       ├── V1Api.java                # Generated API interface
│       └── dto/                      # Generated DTOs
├── .github/workflows/
│   ├── build.yml                     # CI build on all branches
│   └── lint-commit-messages.yml      # Commit message validation
└── docs/manual.adoc                  # User documentation
```

## Critical Rules

### 1. OpenAPI Code Generation
- **NEVER edit files in `target/generated-sources/openapi/`** - they are regenerated on every build
- **To modify API:** Edit `src/main/resources/openapi/r2p-spec.yaml` or `schemas/train.yaml`, then run `mvn compile`
- Generated interfaces are in `ch.rail.r2p.api.V1Api`
- Generated DTOs are in `ch.rail.r2p.api.dto.*`

### 2. Commit Message Format (REQUIRED)
This project uses Conventional Commits with commitlint validation:
```
type(scope): description

types: feat, fix, docs, style, refactor, perf, test, build, ci, chore
```
Examples:
- `feat(api): add new train endpoint`
- `fix(persistence): correct null handling in query`
- `test: add controller tests for edge cases`

**Breaking changes:** Use `!` after type (e.g., `feat!:`) or add `BREAKING CHANGE:` in body.

### 3. Testing Requirements
- All features/bug fixes must include tests
- Tests use `@SpringBootTest` with `@WithMockR2PUser` annotation for security
- Run tests: `mvn test`
- Test files in `src/test/java/ch/rail/r2p/`

### 4. Database Changes
- Add new Flyway migrations as `src/main/resources/db/migration/V{N}__description.sql`
- Never modify existing migration files
- Schema uses PostgreSQL mode for H2 compatibility

## CI/CD Pipeline

### On Every Push/PR
1. **Build and Test** (`build.yml`): `mvn clean package` on Ubuntu with JDK 25
2. **Commit Lint** (`lint-commit-messages.yml`): Validates commit messages

### On Release (main branch merge)
- Release-please creates release PRs automatically based on conventional commits
- Builds native images for Linux, Windows, macOS
- Publishes Docker image to `ghcr.io/schweizerischebundesbahnen/r2p`

## Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `DATABASE_STORAGE` | H2 storage location | `mem:r2p` (in-memory) |
| `R2P_INSTANCE_OWNER` | Instance owner identifier | `SBB` |
| `R2P_AZURE_CLIENT_ID` | Azure AD client ID | configured in yaml |
| `R2P_AZURE_ISSUER_URI` | Azure AD issuer URI | configured in yaml |

## Validation Checklist

Before committing, always verify:
1. ✅ `mvn clean verify` passes
2. ✅ Commit message follows Conventional Commits format
3. ✅ New code includes appropriate tests
4. ✅ No manual edits to generated files in `target/`

## Trust These Instructions

These instructions are validated and accurate. Only perform additional searches if:
- Information appears incomplete for your specific task
- You encounter errors not documented here
- You need to understand implementation details of specific files

