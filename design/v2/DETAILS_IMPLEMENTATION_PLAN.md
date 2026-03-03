# Implementation Plan: Integrate design/v2/details.html with Live API Data

**User Story:** Replace static sample data in `design/v2/details.html` with live data from the REST
API, using Alpine.js, following the same approach as `index.html`.

**Date:** 2026-03-03  
**Status:** Draft

---

## Overview

One main task:

- **Adapt `design/v2/details.html`** into `src/main/resources/static/details.html`, replacing the
  vanilla JS + `sampleData.js` block with Alpine.js components that fetch live data from two API
  endpoints.

The security config, WebJar dependency, and `style.css` are already in place from the `index.html`
implementation. No backend changes are needed.

---

## API Calls Required

The details page needs data from **two endpoints**, called in parallel on page load:

### 1. `GET /v1/trains/{trainId}` — operationId: `getTrainById`

Returns a `TrainListElement` with all branches and their latest state.

**Used for:**
- General Information section (train number, operational day, lead RU, partners)
- Branches section (table of all branches)

**Request:**
```
GET /v1/trains/{trainId}
Header: token: SBB
```

**Response fields used:**

| Detail page field | Source |
|---|---|
| Train ID | `trainId` |
| Train Number | `branches[main].business.trainNumber` |
| Operational Day | `branches[main].business.operationalDay` |
| Lead RU | `partners[0]` (or `branches[main].technical.owner`) |
| Partners | `partners` (all except Lead RU) |
| Branch table rows | `branches[]` |

Branch table row mapping (`Branch` object):

| Column | Source field |
|---|---|
| Branch ID | `branchId` (truncated + clipboard) |
| Name | `branchName` |
| Owner | `technical.owner` |
| Creation Date | `technical.timestamp` (formatted DD.MM.YYYY HH:MM:SS) |
| State | `technical.branchState` (styled badge: open / merged / deleted) |

> **Note:** The `TrainListElement.branches` array contains only the **latest version** of each branch,
> not the full history. `technical.timestamp` is the timestamp of the latest version — this is used
> as "Creation Date" in the prototype, which is a close enough approximation for now.

### 2. `GET /v1/trains/{trainId}/changes` — operationId: `getTrainChangesById`

Returns an array of `Change` objects (the audit log for the train).

**Used for:**
- Changelog section (table of all change events)

**Request:**
```
GET /v1/trains/{trainId}/changes
Header: token: SBB
```

**Response fields used:**

| Column | Source field |
|---|---|
| Action | `action` (styled badge) |
| Timestamp | `timestamp` (formatted DD.MM.YYYY HH:MM:SS) |
| Branch Name | `branchName` |
| Version ID | `version` (truncated + clipboard, optional — may be null) |
| Partner | `partner` (styled partner badge) |
| Comment | `comment` + optional `affectedPartner` appended in parentheses |

### `trainId` source

The `trainId` is read from the URL query parameter: `?trainId=<uuid>`, exactly as the prototype
already does with `new URLSearchParams(window.location.search).get('trainId')`.

If `trainId` is missing or either API call returns 404, the error state is shown.

---

## Alpine.js Component Structure

The entire `<div class="container">` becomes a single `x-data="detailsApp()"` component with
`x-init="init()"`.

```js
function detailsApp() {
    return {
        // state
        trainId: null,
        loading: true,
        error: null,

        // general info
        train: null,          // mapped from TrainListElement

        // branches
        branches: [],         // mapped from TrainListElement.branches

        // changelog
        changelog: [],        // array of Change objects

        async init() {
            this.trainId = new URLSearchParams(window.location.search).get('trainId');
            if (!this.trainId) {
                this.error = 'No trainId provided in URL.';
                this.loading = false;
                return;
            }
            try {
                // Fetch both endpoints in parallel
                const [trainRes, changesRes] = await Promise.all([
                    fetch(`/v1/trains/${this.trainId}`, { headers: { 'token': 'SBB' } }),
                    fetch(`/v1/trains/${this.trainId}/changes`, { headers: { 'token': 'SBB' } })
                ]);

                if (trainRes.status === 404) { this.error = 'Train not found.'; return; }
                if (!trainRes.ok) throw new Error(`Train API: HTTP ${trainRes.status}`);
                if (!changesRes.ok) throw new Error(`Changes API: HTTP ${changesRes.status}`);

                const trainData   = await trainRes.json();
                const changesData = await changesRes.json();

                this.train     = this.mapTrain(trainData);
                this.branches  = this.mapBranches(trainData.branches);
                this.changelog = changesData; // Change objects used as-is

            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        mapTrain(t) { /* ... */ },
        mapBranches(branches) { /* ... */ },

        // formatting helpers (same as index.html)
        formatDate(d) { /* DD.MM.YYYY */ },
        formatTimestamp(d) { /* DD.MM.YYYY HH:MM:SS */ },
        truncateId(id) { /* first 8 chars + '...' */ },
        partnerBadgeClass(p) { /* partner-badge-SBB etc. */ },
        branchStateBadgeClass(state) { /* badge-open / badge-merged / badge-deleted */ },
        actionBadgeClass(action) { /* badge-created / badge-updated / etc. */ },
        copyToClipboard(text) { /* navigator.clipboard.writeText */ },
    };
}
```

---

## Template Changes

### Loading / Error / Content visibility

Replace the `style="display: none;"` / `style.display` vanilla JS toggling with Alpine.js
directives on the existing elements:

| Element | Prototype control | Alpine.js replacement |
|---|---|---|
| `#error-message` div | `style.display = 'block'` | `x-show="!loading && !!error"` |
| `#main-content` div | `style.display = 'block'` | `x-show="!loading && !error && !!train"` |
| (new) loading indicator | not in prototype | `x-show="loading"` paragraph above the sections |

The `id` attributes on the error and content divs can be kept for reference but are no longer
functionally needed.

### General Information section

Replace the `id`-based DOM manipulation (`document.getElementById(...).textContent = ...`) with
inline `x-text` and `:class` bindings:

```html
<span x-text="truncateId(train.trainId)" :title="train.trainId"></span>
<button @click="copyToClipboard(train.trainId)">📋</button>

<div x-text="train.trainNumber"></div>
<div x-text="formatDate(train.operationalDay)"></div>

<!-- Lead RU badge -->
<span class="partner-badge" :class="partnerBadgeClass(train.leadRU)" x-text="train.leadRU"></span>

<!-- Partner badges (excluding Lead RU) -->
<template x-for="p in train.otherPartners" :key="p">
    <span class="partner-badge" :class="partnerBadgeClass(p)" x-text="p"></span>
</template>
```

The page `<title>` and `<h1 id="page-title">` are updated reactively:
```html
<h1 x-text="train ? 'Train Details - ' + train.trainNumber : 'Train Details'"></h1>
```

### Branches section

Replace `<tbody id="branches-tbody">` innerHTML manipulation with `x-for`:

```html
<template x-for="branch in branches" :key="branch.branchId">
    <tr>
        <td class="id-cell">
            <span class="id-truncated" :title="branch.branchId"
                  x-text="truncateId(branch.branchId)"></span>
            <button class="clipboard-btn" @click="copyToClipboard(branch.branchId)"
                    aria-label="Copy branch ID" title="Copy full ID">📋</button>
        </td>
        <td><strong x-text="branch.branchName"></strong></td>
        <td>
            <span class="partner-badge" :class="partnerBadgeClass(branch.owner)"
                  x-text="branch.owner"></span>
        </td>
        <td x-text="formatTimestamp(branch.timestamp)"></td>
        <td>
            <span class="badge" :class="branchStateBadgeClass(branch.state)"
                  x-text="branch.state"></span>
        </td>
    </tr>
</template>
```

The branch filter bar and sort headers are kept as static HTML (not wired up — out of scope,
same treatment as the filter bar in `index.html`). Remove the orphaned `onchange`/`onclick`/
`onkeypress` inline handlers to avoid JS errors.

Pagination for branches: kept as static HTML with all buttons `disabled` (not yet wired up).

### Changelog section

Replace `<tbody id="changelog-tbody">` with `x-for` over `changelog`:

```html
<template x-for="(entry, index) in changelog" :key="index">
    <tr>
        <td>
            <span class="badge" :class="actionBadgeClass(entry.action)"
                  x-text="entry.action"></span>
        </td>
        <td x-text="formatTimestamp(entry.timestamp)"></td>
        <td x-text="entry.branchName ?? '-'"></td>
        <td class="id-cell">
            <template x-if="entry.version">
                <span>
                    <span class="id-truncated" :title="entry.version"
                          x-text="truncateId(entry.version)"></span>
                    <button class="clipboard-btn" @click="copyToClipboard(entry.version)"
                            title="Copy full ID">📋</button>
                </span>
            </template>
            <span x-show="!entry.version" class="text-muted">-</span>
        </td>
        <td>
            <span class="partner-badge" :class="partnerBadgeClass(entry.partner)"
                  x-text="entry.partner"></span>
        </td>
        <td x-text="entry.affectedPartner
                    ? (entry.comment ?? '') + ' (' + entry.affectedPartner + ')'
                    : (entry.comment ?? '-')"></td>
    </tr>
</template>
```

Changelog filter bar: kept as static HTML (not wired up — out of scope). Remove orphaned
inline handlers.

Pagination for changelog: kept as static HTML with all buttons `disabled` (not yet wired up).

---

## Link from index.html

The row click handler in `src/main/resources/static/index.html` already navigates to:
```
details.html?trainId=<uuid>
```
No change needed there — it will work as-is once `details.html` exists in `static/`.

---

## Affected Files

| File | Action |
|---|---|
| `src/main/resources/static/details.html` | **Create** (adapted from `design/v2/details.html`) |

No other files need to change.

---

## Out of Scope

- Branch filter bar (keep static HTML, remove orphan handlers)
- Branch sort headers (keep static HTML, remove orphan handlers)
- Branch pagination (keep static HTML, disabled buttons)
- Changelog filter bar (keep static HTML, remove orphan handlers)
- Changelog pagination (keep static HTML, disabled buttons)
- Merge Requests section (kept as static "not yet implemented" placeholder — unchanged from design)

---

## Implementation Order

1. Create `src/main/resources/static/details.html`:
   a. Copy HTML structure verbatim from `design/v2/details.html`.
   b. Add `x-data="detailsApp()"` + `x-init="init()"` to the container div.
   c. Replace `<script src="sampleData.js">` with `<script defer src="/webjars/alpinejs/dist/cdn.min.js">`.
   d. Replace the inline `<script>` block with the `detailsApp()` Alpine.js component.
   e. Wire up General Information, Branches tbody, and Changelog tbody with Alpine directives.
   f. Remove orphaned inline event handlers from filter/sort/pagination elements.
   g. Set pagination buttons to `disabled`.
2. Verify `index.html` row click still navigates correctly to `details.html?trainId=<uuid>`.

---

## Testing

- Run `mvn clean verify` — no new backend code, so all existing tests must continue to pass.
- Manual smoke test (`mvn spring-boot:run`):
  - Create at least one train via Swagger UI or curl (e.g. `POST /v1/trains`).
  - Click a row in `http://localhost:8080/` → navigates to `details.html?trainId=<uuid>`.
  - General Information section shows correct train number, operational day, Lead RU, partners.
  - Branches section shows all branches with correct owner, state, and timestamp.
  - Changelog section shows all change events with correct action, partner, and comment.
  - Navigating to `details.html` with an invalid/missing `trainId` shows the error state.

