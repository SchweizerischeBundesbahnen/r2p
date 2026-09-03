## Code Language

Code language is always English. Never use German for names, comments, descriptions, documentation, or anything else.

## AsciiDoc – Escaping Path Parameters

When writing URL paths in AsciiDoc that contain path parameter placeholders such as `{trainId}` or `{branchId}`, always escape the opening curly brace with a backslash to prevent AsciiDoc from interpreting them as document attributes:

```asciidoc
`/v1/trains/\{trainId}/branches/\{branchId}`
```

This applies to all `.adoc` files in this repository (`docs/`).

## Rules

* write new tests (or adapt existing ones) for every change
* never accept pre-existing issues. When everything is finished the tests must be green
* when asked if it is ready to be commited, check if there are no open points, run all tests and create a git-commit.txt file with a proposal for a conventional commit git message (body lines limited to 100 characters length) omit trivial things like added tests but include a brief description of new concepts
* use lombok annotations like @Slf4j where appropriate
* when changing or extending the web UI (`src/main/resources/static`), follow the brand and
  component rules in `docs/design-system.md` — reuse its tokens/components, keep it flat,
  vendor-neutral, emoji-free, sentence-case, and write the product name as **R²P** (superscript ²)