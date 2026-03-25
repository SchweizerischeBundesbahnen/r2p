/**
 * r2p-utils.js — Shared pure utility functions for the R2P frontend.
 * No Alpine.js or framework dependency; can be loaded before Alpine initialises.
 */

// ── Formatting ────────────────────────────────────────────────────────────────

/**
 * Formats an ISO date string (YYYY-MM-DD) as DD.MM.YYYY.
 * @param {string|null} dateString
 * @returns {string}
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    const [year, month, day] = dateString.split('-');
    return `${day}.${month}.${year}`;
}

/**
 * Formats an ISO timestamp string as DD.MM.YYYY HH:MM:SS (local time).
 * @param {string|null} dateString
 * @returns {string}
 */
function formatTimestamp(dateString) {
    if (!dateString) return '-';
    const date    = new Date(dateString);
    const day     = String(date.getDate()).padStart(2, '0');
    const month   = String(date.getMonth() + 1).padStart(2, '0');
    const year    = date.getFullYear();
    const hours   = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
}

/**
 * Truncates a UUID/ID to the first 8 characters followed by an ellipsis.
 * @param {string|null} id
 * @returns {string}
 */
function truncateId(id) {
    if (!id) return '-';
    return id.substring(0, 8) + '…';
}

// ── CSS class helpers ─────────────────────────────────────────────────────────

/**
 * Returns the CSS class for a partner badge.
 * @param {string} partner
 * @returns {string}
 */
function partnerBadgeClass(partner) {
    const mapping = {
        'SBB': 'partner-badge-SBB',
        'BLS': 'partner-badge-BLS',
        'SOB': 'partner-badge-SOB'
    };
    return mapping[partner] ?? 'partner-badge-default';
}

/**
 * Returns the CSS class for a branch-state badge.
 * @param {string} state
 * @returns {string}
 */
function branchStateBadgeClass(state) {
    const mapping = {
        'open':             'badge-open',
        'merge_requested':  'badge-merge-requested',
        'merged':           'badge-merged',
        'deleted':          'badge-deleted'
    };
    return mapping[state] ?? 'badge-open';
}

/**
 * Returns the CSS class for a changelog-action badge.
 * @param {string} action
 * @returns {string}
 */
function actionBadgeClass(action) {
    const mapping = {
        'created':          'badge-created',
        'updated':          'badge-updated',
        'merged':           'badge-merged-action',
        'deleted':          'badge-deleted-action',
        'shared':           'badge-shared',
        'unshared':         'badge-unshared',
        'merge_requested':  'badge-merge-requested',
        'merge_rejected':   'badge-deleted-action'
    };
    return mapping[action] ?? 'badge-created';
}

/**
 * Returns the CSS class for a merge-request state badge.
 * Only maps merge-specific actions (merge_requested, merge_rejected, merged).
 * @param {string} action
 * @returns {string}
 */
function mergeRequestBadgeClass(action) {
    const mapping = {
        'merge_requested': 'badge-merge-requested',
        'merge_rejected':  'badge-deleted-action',
        'merged':          'badge-merged-action'
    };
    return mapping[action] ?? 'badge-open';
}

// ── Sort factory ──────────────────────────────────────────────────────────────

/**
 * Creates a composable sorter object for use inside an Alpine.js component.
 * Eliminates duplicated sort boilerplate when a page has multiple sortable tables.
 *
 * Usage (inside an Alpine component's return object):
 *   branchSorter: makeSorter(),
 *   clSorter:     makeSorter('timestamp', 'desc'),
 *
 * Then in the template:
 *   @click="branchSorter.sort('name')"
 *   x-text="branchSorter.indicator('name')"
 *   :aria-sort="branchSorter.ariaSort('name')"
 *   :class="{ 'active-sort': branchSorter.col === 'name' }"
 *
 * @param {string|null} defaultCol
 * @param {'asc'|'desc'} defaultDir
 * @returns {{ col: string|null, dir: string, sort(col:string):void, indicator(col:string):string, ariaSort(col:string):string }}
 */
function makeSorter(defaultCol = null, defaultDir = 'asc') {
    return {
        col: defaultCol,
        dir: defaultDir,

        sort(col) {
            if (this.col === col) {
                this.dir = this.dir === 'asc' ? 'desc' : 'asc';
            } else {
                this.col = col;
                this.dir = 'asc';
            }
        },

        indicator(col) {
            if (this.col !== col) return '';
            return this.dir === 'asc' ? '▲' : '▼';
        },

        ariaSort(col) {
            if (this.col !== col) return 'none';
            return this.dir === 'asc' ? 'ascending' : 'descending';
        }
    };
}

/**
 * Applies a sorter's current state to an array, returning a new sorted array.
 * @template T
 * @param {T[]} items
 * @param {{ col: string|null, dir: string }} sorter
 * @returns {T[]}
 */
function applySorter(items, sorter) {
    if (!sorter.col) return items;
    const dir = sorter.dir === 'asc' ? 1 : -1;
    return [...items].sort((a, b) => {
        const va = (a[sorter.col] ?? '').toString().toLowerCase();
        const vb = (b[sorter.col] ?? '').toString().toLowerCase();
        return va < vb ? -dir : va > vb ? dir : 0;
    });
}

// ── Pagination helper ─────────────────────────────────────────────────────────

/**
 * Builds a human-readable pagination info string.
 * @param {number} total        Total number of (filtered) items
 * @param {number} page         Current page (1-based)
 * @param {number} itemsPerPage
 * @param {string} label        Plural label, e.g. "trains", "branches"
 * @returns {string}
 */
function paginationInfo(total, page, itemsPerPage, label) {
    if (total === 0) return `No ${label} found`;
    const start = (page - 1) * itemsPerPage + 1;
    const end   = Math.min(page * itemsPerPage, total);
    return `Showing ${start}–${end} of ${total} ${label}`;
}

// ── Toast / Clipboard ─────────────────────────────────────────────────────────

/**
 * Shows a transient toast message using the #toast element.
 * @param {string} message
 * @param {number} [duration=2000]
 */
function showToast(message, duration = 2000) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.style.display = 'block';
    setTimeout(() => { toast.style.display = 'none'; }, duration);
}

/**
 * Copies text to the clipboard and shows a toast feedback message.
 * @param {string} text
 */
function copyToClipboard(text) {
    navigator.clipboard.writeText(text)
        .then(() => showToast('Copied to clipboard!'))
        .catch(() => showToast('Failed to copy'));
}

// ── Timetable Year ────────────────────────────────────────────────────────────

/**
 * Computes the start date (inclusive) and end date (inclusive) for a given
 * timetable year.
 *
 * A timetable year starts on the Sunday after the second Saturday of December
 * of the *preceding* calendar year and is named after the *subsequent* year.
 * E.g. timetable year 2026 starts on the Sunday after the second Saturday of
 * December 2025.
 *
 * @param {number} year  The timetable year (e.g. 2026)
 * @returns {{ start: string, end: string }}  ISO dates 'YYYY-MM-DD'
 */
function getTimetableYearBounds(year) {
    return {
        start: _timetableYearStart(year),
        end:   _timetableYearEnd(year)
    };
}

/**
 * Formats a Date object as 'YYYY-MM-DD' using local (wall-clock) date parts,
 * avoiding UTC-offset issues that arise with toISOString().
 * @param {Date} d
 * @returns {string}
 */
function _localDateString(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

/**
 * Returns the ISO start date of a timetable year (non-recursive helper).
 * @param {number} year
 * @returns {string} 'YYYY-MM-DD'
 */
function _timetableYearStart(year) {
    // Find the second Saturday of December in (year - 1)
    const dec1 = new Date(year - 1, 11, 1); // December 1st of preceding year
    // Day-of-week: 0=Sun … 6=Sat
    const dayOfWeek = dec1.getDay();
    // Days until first Saturday
    const daysToFirstSat = (6 - dayOfWeek + 7) % 7;
    const secondSatDay = 1 + daysToFirstSat + 7; // 1-based day of month
    // Start = Sunday after second Saturday
    const start = new Date(year - 1, 11, secondSatDay + 1);
    return _localDateString(start);
}

/**
 * Returns the ISO end date (inclusive) of a timetable year (non-recursive helper).
 * End = day before the start of the next timetable year.
 * @param {number} year
 * @returns {string} 'YYYY-MM-DD'
 */
function _timetableYearEnd(year) {
    const nextStartStr = _timetableYearStart(year + 1);
    const [ny, nm, nd] = nextStartStr.split('-').map(Number);
    const nextStart = new Date(ny, nm - 1, nd);
    nextStart.setDate(nextStart.getDate() - 1);
    return _localDateString(nextStart);
}

/**
 * Returns the integer timetable year that contains today's date.
 * @returns {number}
 */
function getCurrentTimetableYear() {
    const today = _localDateString(new Date());
    const calYear = new Date().getFullYear();
    // Check whether today falls within TY named after current calendar year
    const bounds = getTimetableYearBounds(calYear);
    if (today >= bounds.start) {
        return calYear;
    }
    return calYear - 1;
}

/**
 * Returns an array of timetable year option objects for a dropdown,
 * covering currentTY - 2 through currentTY + 2.
 * @returns {{ value: number, label: string }[]}
 */
function getTimetableYearOptions() {
    const current = getCurrentTimetableYear();
    const options = [];
    for (let y = current - 2; y <= current + 2; y++) {
        options.push({ value: y, label: String(y) });
    }
    return options;
}

// ── Partner token (simulated authentication) ─────────────────────────────────

/**
 * All available partner tokens, matching the PartnerToken enum in the OpenAPI spec.
 * This is a temporary front-end-only simulation until real auth is in place.
 * @type {string[]}
 */
const PARTNER_TOKENS = ['SBB', 'BLS', 'SOB'];

const _TOKEN_STORAGE_KEY = 'r2p_selected_token';

/**
 * Returns the currently selected partner token.
 * Falls back to 'SBB' if nothing is stored.
 * @returns {string}
 */
function getSelectedToken() {
    return localStorage.getItem(_TOKEN_STORAGE_KEY) ?? 'SBB';
}

/**
 * Persists the selected partner token.
 * @param {string} token  Must be one of PARTNER_TOKENS
 */
function setSelectedToken(token) {
    localStorage.setItem(_TOKEN_STORAGE_KEY, token);
}

// ── Data mapping ──────────────────────────────────────────────────────────────

/**
 * Maps a raw API train object to the view model used by both pages.
 * @param {object} t  Raw train object from the API
 * @returns {{ trainId, trainNumber, operationalDay, leadRU, otherPartners }}
 */
function mapTrainBase(t) {
    const mainBranch = (t.branches ?? []).find(b => b.technical?.mainBranch === true)
                       ?? (t.branches ?? [])[0];
    const leadRU = mainBranch?.technical?.owner ?? '?';
    return {
        trainId:        t.trainId,
        trainNumber:    mainBranch?.business?.trainNumber ?? '-',
        operationalDay: mainBranch?.business?.operationalDay ?? null,
        leadRU,
        otherPartners:  (t.partners ?? []).filter(p => p !== leadRU)
    };
}

