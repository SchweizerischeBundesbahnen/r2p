# Copilot Agent Instructions for R2P Repository

## Project Overview

**R2P** (RU-RU-Partner) is a Spring Boot 4.0 prototype for railway (RU - Railway Undertaking) partner communication. It's a medium-sized Java project (~74 source files) using an H2 embedded database, Flyway migrations, OpenAPI code generation, and Azure AD OAuth2 authentication.

**Key Technologies:**
- Java 25, Maven 3.6+, Spring Boot 4.0.2
- H2 Database 2.4.240 (in-memory or file-based), Spring Data JDBC, Flyway
- OpenAPI Generator 7.19.0 (spring server & asciidoc documentation)
- Security: OAuth2/JWT (Azure AD), Spring Security
- Lombok, Jackson, SpringDoc OpenAPI UI 3.0.1
- AsciiDoctor 3.2.0 for documentation (HTML + PDF)
- Node.js/npm for commit linting (commitlint, husky)

## Build Commands (VALIDATED - ALWAYS use these)

### Primary Build & Test (REQUIRED before committing)
```bash
mvn clean verify
```
This runs the full build including:
1. OpenAPI code generation (generates DTOs and API interfaces in `target/generated-sources/openapi/`)
2. Compilation of 74 source files
3. All unit tests (37 tests across 4 test classes)
4. AsciiDoc documentation generation (HTML + PDF for both manual and API docs)
5. JAR packaging

**Build time:** ~40 seconds (validated)
**ALWAYS run this before committing** - it ensures generated code is up-to-date and tests pass.

### Quick Compile (no tests)
```bash
mvn clean package -DskipTests
```
**Build time:** ~36 seconds (validated)
Use when you need to verify compilation without running tests.

### Run Application Locally
```bash
mvn spring-boot:run
```
Access at: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs

**Note:** To access secured endpoints, you must be a member of AD group `DG_SEC_DEV_R2P_Admin`.

### Run Tests Only
```bash
mvn test
```

### Platform-Specific Native Images (jpackage)
- Windows: `mvn clean package -Papp-image-windows`
- Linux/Unix: `mvn clean package -Papp-image-unix`
- macOS: `mvn clean package -Papp-image-mac`

**Important for Windows:** The `app-image-windows` profile includes a cleanup step that removes read-only attributes from `target/dist` before clean.

## Project Structure

```
r2p/
├── pom.xml                           # Maven build configuration (Spring Boot 4.0.2)
├── package.json                      # npm config for commitlint and husky
├── commitlint.config.js              # Conventional commits validation
├── .spectral.yaml                    # Spectral linting rules for OpenAPI validation
├── .release-please-manifest.json     # Release-please version tracking
├── release-please-config.json        # Release-please changelog configuration
├── src/main/java/ch/rail/r2p/
│   ├── R2pApplication.java           # Main entry point (@SpringBootApplication)
│   ├── Constants.java                # Shared constants (e.g., ZURICH_ZONE_ID)
│   ├── config/                       # Spring configuration classes
│   │   ├── SecurityConfig.java       # OAuth2/JWT security setup
│   │   ├── JacksonConfig.java        # JSON serialization config
│   │   ├── JdbcConfig.java           # JDBC configuration
│   │   ├── FlywayConfig.java         # Flyway migration config
│   │   └── OpenApiSecurityConfig.java # OpenAPI security schemes
│   ├── domain/                       # Domain models
│   │   ├── TrainVersion.java         # Core train version entity
│   │   ├── TrainVersionEnriched.java # Enriched version with branch data
│   │   ├── Branch.java               # Branch entity
│   │   └── Changelog.java            # Audit log entity
│   ├── persistence/                  # Data access layer interfaces
│   │   ├── TrainPersistence.java     
│   │   ├── ChangelogPersistence.java
│   │   └── impl/                     # Repository implementations
│   │       ├── TrainPersistenceImpl.java
│   │       ├── ChangelogPersistenceImpl.java
│   │       ├── BranchSpringDataRepository.java
│   │       ├── ChangelogSpringDataRepository.java
│   │       └── CustomInsertRepository.java
│   └── web/                          # REST controllers and services
│       ├── TrainController.java      # Main API controller (implements V1Api)
│       ├── TrainService.java         # Business logic
│       ├── TrainMapper.java          # DTO mapping
│       ├── HomeController.java       # Root endpoint
│       └── GlobalExceptionHandler.java # Exception handling
├── src/main/resources/
│   ├── application.yaml              # Spring Boot configuration
│   ├── db/migration/
│   │   └── V1__create.sql            # Flyway DB schema (4 tables: train_version, branch, train_partner, changelog)
│   └── openapi/
│       ├── r2p-spec.yaml             # OpenAPI specification (SOURCE OF TRUTH)
│       ├── sampleTrain.json          # Sample train data
│       └── schemas/
│           └── train.yaml            # Train data schema (extracted from spec)
├── src/test/java/ch/rail/r2p/
│   ├── R2pApplicationTests.java      # Application context tests
│   ├── WithMockR2PUser.java          # Test annotation for security (@WithMockR2PUser)
│   ├── config/
│   │   └── JwtAuthenticationConverterTests.java
│   └── web/
│       ├── TrainControllerTests.java  # Main controller tests (23 tests)
│       └── TrainControllerChangelogTests.java # Changelog tests (6 tests)
├── target/generated-sources/openapi/ # GENERATED - do not edit manually
│   └── src/main/java/ch/rail/r2p/api/
│       ├── V1Api.java                # Generated API interface
│       ├── dto/                      # Generated DTOs (46 classes)
│       └── ApiUtil.java              # Utility class
├── target/docs/                      # Generated documentation
│   ├── r2p-manual.html               # User manual (HTML)
│   ├── r2p-manual.pdf                # User manual (PDF)
│   ├── r2p-api.html                  # API docs (HTML)
│   └── r2p-api.pdf                   # API docs (PDF)
├── docs/
│   ├── manual.adoc                   # User documentation source
│   └── images/                       # Images for documentation
├── openshift/
│   └── deployment.yaml               # OpenShift deployment configuration
├── .github/workflows/
│   ├── build.yml                     # CI build on all branches (validates OpenAPI + builds)
│   ├── lint-commit-messages.yml      # Commit message validation
│   ├── release-please.yml            # Automated releases on main branch
│   └── automerge-snapshot.yml        # Auto-merge PRs with "autorelease: snapshot" label
└── .husky/
    └── commit-msg                    # Pre-commit hook for commitlint
```

## Critical Rules

### 1. OpenAPI Code Generation
- **NEVER edit files in `target/generated-sources/openapi/`** - they are regenerated on every build
- **To modify API:** Edit `src/main/resources/openapi/r2p-spec.yaml` or `schemas/train.yaml`, then run `mvn compile`
- **OpenAPI Validation:** The spec is validated with Spectral (see `.spectral.yaml`) on every CI build
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

### On Every Push/PR (`.github/workflows/build.yml`)
1. **Validate OpenAPI Spec**: Spectral validates `r2p-spec.yaml` against OpenAPI standards
2. **Build and Test**: `mvn clean package` on Ubuntu with JDK 25 (Temurin distribution)
3. **Upload Artifacts**: Test results (`target/surefire-reports/`) and documentation (`target/docs/`)

### On Every Push/PR (`.github/workflows/lint-commit-messages.yml`)
- Validates commit messages against Conventional Commits specification
- Uses commitlint with `@commitlint/config-conventional`
- For PRs: validates all commits in the PR
- For pushes: validates the last commit

### On Release (main branch merge, `.github/workflows/release-please.yml`)
1. **Release-Please**: Creates release PRs automatically based on conventional commits
2. **Build Native Images**: Creates jpackage app-images for Linux, Windows, and macOS
3. **Upload Release Assets**:
   - `r2p-linux.tar.gz` (native app-image)
   - `r2p-windows.zip` (native app-image)
   - `r2p-macos.tar.gz` (native app-image)
   - `r2p-documentation.zip` (HTML + PDF docs)
   - `r2p-test-results.zip` (combined test results)
4. **Docker Image**: Builds and pushes to `ghcr.io/schweizerischebundesbahnen/r2p` using Spring Boot buildpacks
   - Tags: `latest`, `<version>`, `<major>.<minor>`, `<major>`

### Automerge Snapshot PRs (`.github/workflows/automerge-snapshot.yml`)
- Automatically merges PRs labeled with `autorelease: snapshot`
- Uses squash merge and deletes branch after merge

### Release Strategy
- Release-please tracks versions in `.release-please-manifest.json` (current: 0.0.11)
- Changelog configuration in `release-please-config.json`
- Supports all conventional commit types: feat, fix, docs, chore, refactor, test, build, ci

## Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `DATABASE_STORAGE` | H2 storage location | `mem:r2p` (in-memory) |
| `FLYWAY_CLEAN_ON_VALIDATION_ERROR` | Auto-clean DB on validation error | `false` |
| `R2P_INSTANCE_OWNER` | Instance owner identifier | `SBB` |
| `R2P_AZURE_CLIENT_ID` | Azure AD client ID | `f5b54c65-e0b1-4233-bc60-bf4c59f399bd` |
| `R2P_AZURE_ISSUER_URI` | Azure AD issuer URI | `https://login.microsoftonline.com/2cda5d11-f0ac-46b3-967d-af1b2e1bd01a` |

**Database Storage Examples:**
- In-memory: `mem:r2p` (default, data lost on restart)
- File-based: `file:C:/devsbb/h2/r2p` (persisted to disk)

## Pre-Commit Hook Setup (RECOMMENDED)

To enable automatic commit message validation locally:
```bash
npm install
```
This installs husky and commitlint, which will validate commit messages before allowing commits.

## Validation Checklist

Before committing, always verify:
1. ✅ `mvn clean verify` passes (full build + tests)
2. ✅ Commit message follows Conventional Commits format
3. ✅ New code includes appropriate tests
4. ✅ No manual edits to generated files in `target/`
5. ✅ OpenAPI spec changes are in `src/main/resources/openapi/r2p-spec.yaml` or `schemas/train.yaml`

## Known Build Warnings (SAFE TO IGNORE)

The following warnings appear during normal builds and are safe to ignore:

1. **OpenAPI Generator Lambda Warning**: `base part for include markup lambda not found: null`
   - Appears during asciidoc generation
   - Does not affect functionality

2. **H2 Version Warning**: `Using H2 2.4.240 which is newer than the version Flyway has been verified with`
   - H2 2.4.240 works correctly with Flyway
   - No action needed

3. **Deprecated API Warning**: `Some input files use or override a deprecated API` in generated `Train.java`
   - Caused by OpenAPI Generator
   - Cannot be fixed manually (file is generated)

4. **AsciiDoc PDF Warning**: `table cell on page 16 has been truncated`
   - Occurs in API documentation PDF generation
   - Does not prevent build or affect usability

5. **Java Agent Warnings** (in tests): Mockito self-attaching warnings
   - Normal behavior for Spring Boot 4.0 tests with Java 25
   - Does not affect test execution

## Known Issues and Workarounds

### Issue: Windows jpackage read-only files
**Problem**: On Windows, jpackage creates read-only files in `target/dist` that prevent `mvn clean` from working.

**Workaround**: The `app-image-windows` profile includes an automatic cleanup step:
```xml
<execution>
    <id>unset-readonly-before-clean</id>
    <phase>pre-clean</phase>
    <configuration>
        <target>
            <exec executable="cmd">
                <arg value="/c"/>
                <arg value="if exist &quot;${project.build.directory}\dist&quot; attrib -R /S /D &quot;${project.build.directory}\dist\*&quot;"/>
            </exec>
        </target>
    </configuration>
</execution>
```
This automatically removes read-only attributes before cleaning.

### Issue: PlantUML diagrams in manual.adoc
**Problem**: `unknown style for listing block: plantuml` warning appears during documentation build.

**Status**: Known issue, does not prevent documentation generation. PlantUML support would require additional AsciiDoctor plugin configuration.

### Code TODOs (for future improvement)
1. **TrainControllerTests.java**: Split large test class into smaller focused test classes (TrainControllerBranchTests, TrainControllerSharingTests, etc.)
2. **TrainService.java**: Implement security checks using Spring Security instead of manual validation
3. **train.yaml**: Add description for some schema properties

## Data model (train.yaml)

Short overview for AI agents of the OpenAPI schema `src/main/resources/openapi/schemas/train.yaml`:
- `Train` is the root object (id, references, links, name, startdate, timestamp, journey).
- `Journey` contains an ordered list of `Journeylocation` entries (at least 2) that describe the sequence of the journey.
- `Journeylocation` consists of `locations`, `activities`, and `requirements` and represents a segment/location within the journey.
- `Activity` models actions using `type`, `timings`, and responsibility/alignment; optionally it can reference `capability` and `requirement`.
- `Capability` and `Requirement` describe required/offered characteristics via `featuresets` and `features` (using `Feature`, `Restriction`, `Condition`).
- Rolling stock is modeled in `RollingStock` and can contain `wagonGroups` or `units` (with `WagonGroup`, `Unit`, `Carbody`).
- Many fields are defined as enums (for example `ActivityType`, `LocationType`, `PlanStatus`, `TimingType`).

## Trust These Instructions

These instructions are validated and accurate. Only perform additional searches if:
- Information appears incomplete for your specific task
- You encounter errors not documented here
- You need to understand implementation details of specific files

**All build commands have been validated on Windows with:**
- Java 25 (Temurin distribution)
- Maven 3.9
- Node.js/npm for commitlint
- Build times measured and verified

**All workflow configurations have been verified from actual `.github/workflows/` files.**

## Additional manual notes

* Only the "Lead RU" can create a new train and update the main branch
* As soon as the "Lead RU" shares the train with a partner, that partner can create/update branches and see the history. It can also do changes in his own branches.
* EVU = Eisenbahnverkehrsunternehmen