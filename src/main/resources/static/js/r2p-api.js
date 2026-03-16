/**
 * r2p-api.js — Centralised API fetch helpers for the R2P frontend.
 * Depends on: r2p-utils.js (getSelectedToken, handleUnauthorized)
 */

// ── Shared request options ────────────────────────────────────────────────────

/**
 * Redirects the browser to the Azure OAuth2 login page.
 */
function handleUnauthorized() {
    window.location.href = '/oauth2/authorization/azure';
}

/**
 * Returns the current API request headers, reading the selected partner token
 * from localStorage on every call so changes take effect immediately.
 * @returns {{ token: string }}
 */
function apiHeaders() {
    return { 'token': getSelectedToken() };
}

// ── Fetch helpers ─────────────────────────────────────────────────────────────

/**
 * Fetches a URL with the shared API headers.
 * Automatically redirects on 401.
 * @param {string} url
 * @returns {Promise<Response>}  Resolves to the Response (may not be ok — caller must check)
 */
async function apiFetch(url) {
    const response = await fetch(url, { headers: apiHeaders() });
    if (response.status === 401) {
        handleUnauthorized();
        // Return a never-resolving promise so callers don't continue after redirect
        return new Promise(() => {});
    }
    return response;
}

/**
 * Fetches the list of trains, optionally filtered by operational day range.
 * @param {{ operationalDayFrom?: string, operationalDayTo?: string }} [params]
 * @returns {Promise<Response>}
 */
async function fetchTrainList(params = {}) {
    const qs = new URLSearchParams();
    if (params.operationalDayFrom) qs.set('operationalDayFrom', params.operationalDayFrom);
    if (params.operationalDayTo)   qs.set('operationalDayTo',   params.operationalDayTo);
    const url = '/v1/trains' + (qs.toString() ? '?' + qs.toString() : '');
    return apiFetch(url);
}

/**
 * Fetches the full details for a single train.
 * @param {string} trainId
 * @returns {Promise<Response>}
 */
async function fetchTrainDetail(trainId) {
    return apiFetch(`/v1/trains/${trainId}`);
}

/**
 * Fetches the changelog (changes) for a single train.
 * @param {string} trainId
 * @returns {Promise<Response>}
 */
async function fetchTrainChanges(trainId) {
    return apiFetch(`/v1/trains/${trainId}/changes`);
}

/**
 * Fetches a specific train version by version ID (getTrainVersion).
 * @param {string} version  UUID of the version
 * @returns {Promise<Response>}
 */
async function fetchTrainVersion(version) {
    return apiFetch(`/v1/trains/versions/${version}`);
}

/**
 * Fetches the branch details (including latest version ID) for a specific branch.
 * @param {string} trainId
 * @param {string} branchId
 * @returns {Promise<Response>}
 */
async function fetchTrainBranch(trainId, branchId) {
    return apiFetch(`/v1/trains/${trainId}/branches/${branchId}`);
}

/**
 * Fetches partner statistics from the backend.
 * Returns a list with one entry per partner plus a totals row (partner === null).
 * @returns {Promise<Response>}
 */
async function fetchStats() {
    return apiFetch('/internal/stats');
}
