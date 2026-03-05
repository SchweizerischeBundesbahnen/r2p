/**
 * r2p-api.js — Centralised API fetch helpers for the R2P frontend.
 * Depends on: r2p-utils.js (for handleUnauthorized is inlined here)
 */

// ── Shared request options ────────────────────────────────────────────────────

const API_HEADERS = { 'token': 'SBB' };

/**
 * Redirects the browser to the Azure OAuth2 login page.
 */
function handleUnauthorized() {
    window.location.href = '/oauth2/authorization/azure';
}

// ── Fetch helpers ─────────────────────────────────────────────────────────────

/**
 * Fetches a URL with the shared API headers.
 * Automatically redirects on 401.
 * @param {string} url
 * @returns {Promise<Response>}  Resolves to the Response (may not be ok — caller must check)
 */
async function apiFetch(url) {
    const response = await fetch(url, { headers: API_HEADERS });
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

