# AGENTS.md

## Project layout

- Single-module Maven project (no sub-modules), Spring Boot, Java 25 (`java.version` in `pom.xml`). There is also a root `package.json`, used only for commit linting (husky/commitlint), not for the app build.
- Sources: `src/main/java/ch/rail/r2p`, tests: `src/test/java/ch/rail/r2p`.
- API DTOs and `V1Api` are **generated** from `src/main/resources/openapi/r2p-spec.yaml` (and `schemas/`) into `target/generated-sources/openapi`. Never edit them. Change the spec instead.

## Build and test

- Command: `mvn -B -ntp verify` (about 1-2 min). It compiles, generates the OpenAPI code, runs all tests, builds the HTML/PDF docs, and repackages the jar.
- Use the wrapper `.\mvnw.cmd` if `mvn` is not on the PATH. A JDK 25 must be active (`JAVA_HOME`).
- Quicker: `mvn -B -ntp test`, or `-Dtest=ClassName` for a single class.
- `mvn` can exit with code 1 in PowerShell even when the build succeeds, because stderr output (such as the `java -version` banner) is treated as an error. Trust the `BUILD SUCCESS` / `Tests run:` lines, not the exit code.
- Pipe output through `Select-Object -Last 60`. Test logs are extremely verbose. The summary is at the end, and per-class results are in `target/surefire-reports`.

## Pitfalls and expected noise (not failures)

- Log lines such as `ERROR ... Skipping train version ... payload deserialization error ... 'tttreftrain'` come from tests that deliberately insert invalid payloads. Ignore them.
- Hibernate Validator `HV000271` warnings come from generated DTOs.
- `asciidoctor-pdf` may log `ERROR: ... table cell ... has been truncated` for the generated API doc. This is non-fatal and the build still succeeds.
- Docs tooling runs as part of `verify`. In `.adoc` files, escape path parameters as `\{trainId}` (see `.github/copilot-instructions.md`).
- `target/` and `data/*.mv.db` (local H2 database) are build or runtime artifacts. Do not hand-edit them.
- The version in `pom.xml` is managed by release-please. Do not bump it manually.
- The `app-image-*` profiles (jpackage) are OS-specific and are not needed for build and test.

## Conventions

- Code, comments, and docs are in English only.
- Commits must follow Conventional Commits (enforced by commitlint and a CI workflow).
- Tests use Spring Boot tests. `NoAuthSpringBootTest` and `WithMockR2PUser` provide the auth-less and mock-user setups. Reuse them for new web tests.
- DB schema changes go in a new Flyway migration under `src/main/resources/db/migration` (`V<n>__name.sql`). Never edit existing migrations.
- Write new tests (or adapt existing ones) for every change
- Never accept pre-existing issues. When everything is finished the tests must be green
- When asked if it is ready to be committed, check if there are no open points, run all tests and create a git-commit.txt file with a proposal for a conventional commit git message (body lines limited to 100 characters length) omit trivial things like added tests but include a brief description of new concepts
- Use lombok annotations like @Slf4j where appropriate
- when changing or extending the web UI (`src/main/resources/static`), follow the brand and
  component rules in `docs/design-system.md` — reuse its tokens/components, keep it flat,
  vendor-neutral, emoji-free, sentence-case, and write the product name as **R²P** (superscript ²)

## AsciiDoc – Escaping Path Parameters

When writing URL paths in AsciiDoc that contain path parameter placeholders such as `{trainId}` or `{branchId}`, always escape the opening curly brace with a backslash to prevent AsciiDoc from interpreting them as document attributes:

```asciidoc
`/v1/trains/\{trainId}/branches/\{branchId}`
```

This applies to all `.adoc` files in this repository (`docs/`).
