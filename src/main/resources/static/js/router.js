/**
 * router.js — Centralised client-side navigation for R2P.
 *
 * All window.location.href assignments live here so that page transitions
 * are easy to find, test, and change in one place.
 *
 * Depends on: nothing (no other r2p scripts required).
 */

const Router = (() => {

    /**
     * Returns the URL for the train details page for a given trainId.
     * Use this when you need the URL as a string (e.g. for an href attribute).
     * @param {string} trainId
     * @returns {string}
     */
    function detailsUrl(trainId) {
        return `details.html?trainId=${encodeURIComponent(trainId)}`;
    }

    /**
     * Navigate to the train details page for a given trainId.
     * @param {string} trainId
     */
    function goToDetails(trainId) {
        window.location.href = detailsUrl(trainId);
    }

    /**
     * Navigate to the train-version page showing the latest version of a branch.
     * @param {string} trainId
     * @param {string} branchId
     * @param {string} branchName
     * @param {string|null} [trainNumber]
     * @param {string|null} [operationalDay]
     */
    function goToBranchVersion(trainId, branchId, branchName, trainNumber, operationalDay) {
        const p = { trainId, branchId, branchName };
        if (trainNumber)    p.trainNumber    = trainNumber;
        if (operationalDay) p.operationalDay = operationalDay;
        window.location.href = `train-version.html?${new URLSearchParams(p).toString()}`;
    }

    /**
     * Navigate to the train-version page for a specific version UUID.
     * @param {string} versionId
     * @param {string} trainId
     * @param {string|null} [branchName]
     * @param {string|null} [branchId]
     * @param {string|null} [trainNumber]
     * @param {string|null} [operationalDay]
     */
    function goToVersion(versionId, trainId, branchName, branchId, trainNumber, operationalDay) {
        const p = { version: versionId, trainId };
        if (branchName)     p.branchName     = branchName;
        if (branchId)       p.branchId       = branchId;
        if (trainNumber)    p.trainNumber    = trainNumber;
        if (operationalDay) p.operationalDay = operationalDay;
        window.location.href = `train-version.html?${new URLSearchParams(p).toString()}`;
    }

    /**
     * Redirect the browser to the Azure AD OAuth2 authorisation endpoint.
     */
    function goToLogin() {
        window.location.href = '/oauth2/authorization/azure';
    }

    return { detailsUrl, goToDetails, goToBranchVersion, goToVersion, goToLogin };
})();



