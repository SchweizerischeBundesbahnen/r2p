# Copilot Instructions

## AsciiDoc – Escaping Path Parameters

When writing URL paths in AsciiDoc that contain path parameter placeholders such as `{trainId}` or `{branchId}`, always escape the opening curly brace with a backslash to prevent AsciiDoc from interpreting them as document attributes:

```asciidoc
`/v1/trains/\{trainId}/branches/\{branchId}`
```

This applies to all `.adoc` files in this repository (`docs/`).

