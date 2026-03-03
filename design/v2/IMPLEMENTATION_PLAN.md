# Implementation Plan: Integrate design/v2 UI into Spring Boot Application

**User Story:** Integrate the validated `design/v2/index.html` prototype as a live static resource in the
application, backed by real data from the REST API via Alpine.js.

**Date:** 2026-03-03  
**Status:** Draft

---

## Overview

Three distinct tasks:

1. **Make the UI available as a static resource** – copy & adapt `design/v2/index.html` and `style.css`
   into `src/main/resources/static/`.
2. **Temporarily disable backend security** for all routes so the static page can call the REST API
   without an OAuth2 token.
3. **Replace sample data with live API data** – rewrite the JavaScript section using Alpine.js to call
   `GET /v1/trains` and render the train table.

Out of scope (per user story): `details.html`, statistics section, filter bar, search/sort functionality.

---

## Task 1 – Static Resource Setup

### 1.1 Copy HTML and CSS

| Source | Destination |
|---|---|
| `design/v2/index.html` | `src/main/resources/static/index.html` |
| `design/v2/style.css` | `src/main/resources/static/style.css` |

`sampleData.js` is **not** copied – it will no longer be needed.

Spring Boot serves everything under `src/main/resources/static/` automatically at the context root,
so `http://localhost:8080/` will serve `index.html` without any controller changes.

> **Note:** The existing `HomeController.java` currently handles `/`. Check whether it returns a
> redirect or a view – if it conflicts, it must be updated to redirect to the static file or removed.
> Since the static file is served by Spring's default resource handler, the controller mapping for `/`
> should simply be removed or changed to redirect to `/index.html`.

### 1.2 Include Alpine.js via WebJars (Maven dependency)

Add the Alpine.js WebJar to `pom.xml` as a runtime dependency:

```xml
<dependency>
    <groupId>org.webjars.npm</groupId>
    <artifactId>alpinejs</artifactId>
    <version>3.15.8</version>
</dependency>
```

Spring Boot's `WebMvcAutoConfiguration` automatically registers `/webjars/**` as a resource handler
that serves content from `META-INF/resources/webjars/` inside any JAR on the classpath.
The Alpine.js CDN build (the correct entry point for browser use) is packaged at:

```
/webjars/alpinejs/dist/cdn.min.js
```

Reference it in `index.html` with a `defer` `<script>` tag:

```html
<script defer src="/webjars/alpinejs/dist/cdn.min.js"></script>
```

**Why WebJars instead of a manually downloaded file:**
- Version is declared and pinned in `pom.xml` alongside all other dependencies.
- No binary file committed to the repository.
- `mvn clean verify` keeps everything reproducible without a separate download step.
- The SecurityConfig already permits `/webjars/**` implicitly via Spring Boot's default resource
  handler (no additional security rule needed — but verify and add `.requestMatchers("/webjars/**").permitAll()`
  to be explicit).

This avoids any CDN dependency at runtime, which is consistent with the ADR decision
(see `docs/adr/ADR-0001-ui-technology.md`, "Can serve Alpine.js locally").

### 1.3 Clean up the HTML

- Remove the `<script src="sampleData.js"></script>` line.
- Remove the large inline `<script>` block (vanilla JS – will be replaced by Alpine.js in Task 3).
- Keep all HTML structure and CSS classes intact – the design is validated and must not change.
- Remove `onchange`, `oninput`, `onclick` inline event handlers from filter/sort elements
  (they belonged to the old vanilla JS – they will be replaced with Alpine.js `x-on` directives
  in Task 3, but since filters/sort are out of scope now, just remove the orphan references to avoid
  JS errors).

---

## Task 2 – Temporarily Disable Backend Security

### 2.1 Change `SecurityConfig.java`

Modify `src/main/java/ch/rail/r2p/config/SecurityConfig.java` so that **all requests are permitted**
(no OAuth2 resource server requirement).

Specifically, replace the `anyRequest().access(scopeAndRole)` rule with `anyRequest().permitAll()`
and **comment out** (do not delete) the `oauth2ResourceServer` configuration. This makes it easy to
re-enable later.

Add a clearly visible `// TODO: re-enable security` comment.

**CORS:** The static page and the API run on the same origin (`localhost:8080`), so no CORS
configuration is needed.

### 2.2 What stays unchanged

- The `token` header parameter in the OpenAPI spec and controller stays as-is.
- The `GET /v1/trains` endpoint internally reads the `token` header to identify the partner.
  With security disabled, the raw value of the header is used as the partner identifier.
- For the temporary open state, the UI must send a hardcoded `token` header value (e.g. `SBB`).
  This is acceptable for the temporary, security-disabled state.

---

## Task 3 – Populate the Train Table with Alpine.js

### 3.1 API call

Use `GET /v1/trains` (operationId: `listTrains`).

**Request:**
```
GET /v1/trains
Header: token: SBB
```

**Response:** `array of TrainListElement`

```json
[
  {
    "trainId": "uuid",
    "mainTimestamp": "2026-02-13T10:00:00Z",
    "partners": ["SBB", "BLS"],
    "branches": [
      {
        "branchId": "uuid",
        "branchName": "SBB-main",
        "business": {
          "trainNumber": "IC 1234",
          "operationalDay": "2026-02-13"
        },
        "technical": {
          "owner": "SBB",
          "version": "uuid",
          "branchState": "open",
          "timestamp": "2026-02-13T10:00:00Z",
          "comment": "..."
        }
      }
    ]
  }
]
```

### 3.2 Data mapping: API → Table row

Each `TrainListElement` maps to one table row. The **main branch** is identified by the branch whose
`technical.owner` matches the lead RU (i.e. the first partner in `partners`, or the branch named
`*-main`). Display values are derived as follows:

| Table column | Source field |
|---|---|
| Train Number | `branches[main].business.trainNumber` |
| Operational Day | `branches[main].business.operationalDay` (formatted DD.MM.YYYY) |
| Lead RU | First element of `partners` (or `technical.owner` of the main branch) |
| Partners | All entries of `partners` except the Lead RU |
| Status | Count of `open` branches (> 1 means partner branches exist) |
| Last Change | `mainTimestamp` (formatted DD.MM.YYYY HH:MM:SS) |

> **Note on "main branch" identification:** The API response does not flag which branch is the main
> branch explicitly. A reasonable heuristic is the branch whose `branchName` ends with `-main`
> (e.g. `SBB-main`). This matches the domain rules ("Lead RU creates a main branch").

### 3.3 Alpine.js component structure

Replace the entire `<script>` block and the `<tbody id="trains-tbody">` content with a single
Alpine.js `x-data` component on the wrapping `<div class="container">` (or a dedicated wrapper
element).

**Sketch of the Alpine.js component:**

```js
function trainApp() {
  return {
    trains: [],
    loading: true,
    error: null,

    async init() {
      try {
        const response = await fetch('/v1/trains', {
          headers: { 'token': 'SBB' }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        this.trains = data.map(t => this.mapTrain(t));
      } catch (e) {
        this.error = e.message;
      } finally {
        this.loading = false;
      }
    },

    mapTrain(t) {
      const mainBranch = t.branches.find(b => b.branchName.endsWith('-main'))
                         ?? t.branches[0];
      const leadRU = t.partners?.[0] ?? mainBranch?.technical?.owner ?? '?';
      const otherPartners = (t.partners ?? []).filter(p => p !== leadRU);
      const openBranches = t.branches.filter(b => b.technical.branchState === 'open').length;
      return {
        trainId: t.trainId,
        trainNumber: mainBranch?.business?.trainNumber ?? '-',
        operationalDay: mainBranch?.business?.operationalDay ?? '-',
        leadRU,
        otherPartners,
        openBranches,
        lastChange: t.mainTimestamp
      };
    },

    formatDate(d) { /* DD.MM.YYYY */ },
    formatTimestamp(d) { /* DD.MM.YYYY HH:MM:SS */ },
    partnerBadgeClass(p) { /* return CSS class */ }
  };
}
```

**Template changes in `<tbody>`:**

```html
<tbody>
  <template x-if="loading">
    <tr><td colspan="6">Loading…</td></tr>
  </template>
  <template x-if="error">
    <tr><td colspan="6" class="text-error" x-text="'Error: ' + error"></td></tr>
  </template>
  <template x-for="train in trains" :key="train.trainId">
    <tr class="clickable" ...>
      <td><strong x-text="train.trainNumber"></strong></td>
      <td x-text="formatDate(train.operationalDay)"></td>
      <td>
        <span class="partner-badge" :class="partnerBadgeClass(train.leadRU)" x-text="train.leadRU"></span>
      </td>
      <td>
        <template x-for="p in train.otherPartners" :key="p">
          <span class="partner-badge" :class="partnerBadgeClass(p)" x-text="p"></span>
        </template>
        <span x-show="train.otherPartners.length === 0" class="text-muted">None</span>
      </td>
      <td>
        <span x-show="train.openBranches > 1" class="indicator-icon"
              :title="train.openBranches + ' open branch(es)'">
          🌿 <span x-text="train.openBranches"></span>
        </span>
        <span x-show="train.openBranches <= 1" class="text-muted">-</span>
      </td>
      <td x-text="formatTimestamp(train.lastChange)"></td>
    </tr>
  </template>
</tbody>
```

> **Pagination:** Keep the pagination HTML in the DOM (so the design is preserved) but leave it
> static for now (it is out of scope). A comment can be added noting it is not yet wired up.

### 3.4 Loading and error states

- While `loading === true`: show a single row with "Loading trains…" (colspan 6).
- On fetch error: show a single row with the error message in a visible color.
- These replace the "Data will be populated by JavaScript" comment that is currently in the
  prototype's `<tbody>`.

---

## Affected Files

| File | Action |
|---|---|
| `src/main/resources/static/index.html` | **Create** (adapted from `design/v2/index.html`) |
| `src/main/resources/static/style.css` | **Create** (copy of `design/v2/style.css`) |
| `pom.xml` | **Modify** (add `org.webjars.npm:alpinejs:3.14.9` dependency) |
| `src/main/java/ch/rail/r2p/config/SecurityConfig.java` | **Modify** (temporarily disable security) |
| `src/main/java/ch/rail/r2p/web/HomeController.java` | **Modify** (remove or adjust `/` mapping) |

---

## Out of Scope (per User Story)

- `details.html` page
- Statistics section at the top of the page
- Filter bar and search functionality
- Sorting
- Pagination (keep HTML, but not wired up)

---

## Implementation Order

1. Copy and adapt static files (Task 1) – no backend changes, immediately verifiable in the browser
   after `mvn spring-boot:run`.
2. Disable security (Task 2) – single-line change, enables the API to be called from the browser
   without a token.
3. Implement Alpine.js data loading (Task 3) – replace the vanilla JS block with the Alpine.js
   component.

---

## Testing

- Run `mvn clean verify` to confirm the build and existing tests still pass after the security change.
- Manual smoke test: start the application (`mvn spring-boot:run`) and verify:
  - `http://localhost:8080/` serves the HTML page.
  - The train table is populated with data from the running backend.
  - Loading and error states display correctly.
- Existing `TrainControllerTests` and `TrainControllerChangelogTests` must continue to pass
  (they use `@WithMockR2PUser` which bypasses the actual security config).

---

## Re-enabling Security (Future Task)

To re-enable security later:
1. Revert `SecurityConfig.java` (restore `anyRequest().access(scopeAndRole)` and the
   `oauth2ResourceServer` block).
2. Update the `fetch()` call in the Alpine.js component to pass a real Bearer token (obtained via
   the OAuth2 login flow).
3. Remove the hardcoded `token: SBB` header from the Alpine.js fetch call, or make it configurable.

