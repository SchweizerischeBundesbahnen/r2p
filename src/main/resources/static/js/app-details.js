/**
 * app-details.js — Alpine.js component for details.html (train detail page).
 * Depends on: r2p-utils.js, r2p-api.js
 */

function detailsApp() {
    return {
        // ── Core state ────────────────────────────────────────────────────────
        trainId: null,
        loading: true,
        error:   null,
        train:   null,

        // ── Raw data ──────────────────────────────────────────────────────────
        allBranches:  [],
        allChangelog: [],

        // ── Partner token selector ────────────────────────────────────────────
        selectedToken: getSelectedToken(),
        partnerTokens: PARTNER_TOKENS,

        // ── Items per page (shared across both tables) ────────────────────────
        itemsPerPage: 20,

        // ── Branch filter / sorter / page ─────────────────────────────────────
        branchFilterStatus: '',
        branchFilterOwner:  '',
        branchSorter:       makeSorter(),
        branchPage:         1,

        // ── Changelog filter / sorter / page ──────────────────────────────────
        clFilterAction:  '',
        clFilterPartner: '',
        clFilterComment: '',
        clSorter:        makeSorter('timestamp', 'desc'),   // newest first by default
        clPage:          1,

        // ── Derived: branch owners dropdown ───────────────────────────────────
        get branchOwners() {
            return [...new Set(this.allBranches.map(b => b.owner))].sort();
        },

        // ── Derived: changelog partners dropdown ───────────────────────────────
        get clPartners() {
            return [...new Set(this.allChangelog.map(e => e.partner))].sort();
        },

        // ── Derived: filtered + sorted branches ───────────────────────────────
        get filteredBranches() {
            let r = this.allBranches;
            if (this.branchFilterStatus)
                r = r.filter(b => b.state === this.branchFilterStatus);
            if (this.branchFilterOwner)
                r = r.filter(b => b.owner === this.branchFilterOwner);
            return applySorter(r, this.branchSorter);
        },

        get branchTotalPages() {
            return Math.max(1, Math.ceil(this.filteredBranches.length / this.itemsPerPage));
        },

        get pagedBranches() {
            const start = (this.branchPage - 1) * this.itemsPerPage;
            return this.filteredBranches.slice(start, start + this.itemsPerPage);
        },

        // ── Derived: filtered + sorted changelog ───────────────────────────────
        get filteredChangelog() {
            let r = this.allChangelog;
            if (this.clFilterAction)
                r = r.filter(e => e.action === this.clFilterAction);
            if (this.clFilterPartner)
                r = r.filter(e => e.partner === this.clFilterPartner);
            if (this.clFilterComment.trim()) {
                const q = this.clFilterComment.trim().toLowerCase();
                r = r.filter(e => (e.comment ?? '').toLowerCase().includes(q));
            }
            return applySorter(r, this.clSorter);
        },

        get clTotalPages() {
            return Math.max(1, Math.ceil(this.filteredChangelog.length / this.itemsPerPage));
        },

        get pagedChangelog() {
            const start = (this.clPage - 1) * this.itemsPerPage;
            return this.filteredChangelog.slice(start, start + this.itemsPerPage);
        },

        // ── Lifecycle ─────────────────────────────────────────────────────────
        async init() {
            this.trainId = new URLSearchParams(window.location.search).get('trainId');
            if (!this.trainId) {
                this.error   = 'No trainId provided in URL. Please return to the overview and select a train.';
                this.loading = false;
                return;
            }
            try {
                const [trainRes, changesRes] = await Promise.all([
                    fetchTrainDetail(this.trainId),
                    fetchTrainChanges(this.trainId)
                ]);

                if (trainRes.status === 404) {
                    this.error = `Train with ID ${this.trainId} was not found.`;
                    return;
                }
                if (!trainRes.ok)
                    throw new Error(`Train API returned HTTP ${trainRes.status} ${trainRes.statusText}`);
                if (!changesRes.ok)
                    throw new Error(`Changes API returned HTTP ${changesRes.status} ${changesRes.statusText}`);

                const trainData   = await trainRes.json();
                const changesData = await changesRes.json();

                this.train        = this.mapTrain(trainData);
                this.allBranches  = this.mapBranches(trainData.branches ?? []);
                this.allChangelog = changesData;

                document.title = `Train Details - ${this.train.trainNumber} - R2P`;
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        // ── Called when token selector changes ────────────────────────────────
        onTokenChange() {
            setSelectedToken(this.selectedToken);
            this.loading = true;
            this.error   = null;
            this.train   = null;
            this.init();
        },

        // ── Branch sort helpers (delegated to branchSorter) ───────────────────
        branchSort(col) {
            this.branchSorter.sort(col);
            this.branchPage = 1;
        },

        branchSortIndicator(col) {
            return this.branchSorter.indicator(col);
        },

        branchSortAriaSort(col) {
            return this.branchSorter.ariaSort(col);
        },

        branchPaginationInfo() {
            return paginationInfo(
                this.filteredBranches.length,
                this.branchPage,
                this.itemsPerPage,
                'branches'
            );
        },

        // ── Changelog sort helpers (delegated to clSorter) ────────────────────
        clSort(col) {
            this.clSorter.sort(col);
            this.clPage = 1;
        },

        clSortIndicator(col) {
            return this.clSorter.indicator(col);
        },

        clSortAriaSort(col) {
            return this.clSorter.ariaSort(col);
        },

        clPaginationInfo() {
            return paginationInfo(
                this.filteredChangelog.length,
                this.clPage,
                this.itemsPerPage,
                'entries'
            );
        },

        // ── Data mapping ──────────────────────────────────────────────────────
        mapTrain(t) {
            return mapTrainBase(t);
        },

        mapBranches(branches) {
            return branches.map(b => ({
                branchId:  b.branchId,
                name:      b.branchName,
                owner:     b.technical?.owner ?? '-',
                timestamp: b.technical?.timestamp ?? null,
                state:     b.technical?.branchState ?? '-'
            }));
        },

        // ── Navigation helpers ────────────────────────────────────────────
        /**
         * Opens the train-version page showing the latest version of a branch.
         * @param {{ branchId: string, name: string }} branch
         */
        openBranchVersion(branch) {
            const p = {
                trainId:        this.trainId,
                branchId:       branch.branchId,
                branchName:     branch.name,
            };
            if (this.train?.trainNumber)    p.trainNumber    = this.train.trainNumber;
            if (this.train?.operationalDay) p.operationalDay = this.train.operationalDay;
            window.location.href = `train-version.html?${new URLSearchParams(p).toString()}`;
        },

        /**
         * Opens the train-version page for a specific version UUID (called from changelog).
         * @param {string} versionId
         * @param {object} entry  changelog entry (has branchName, branchId)
         */
        openVersionViewer(versionId, entry) {
            const p = {
                version: versionId,
                trainId: this.trainId,
            };
            if (entry?.branchName)          p.branchName     = entry.branchName;
            if (entry?.branchId)            p.branchId       = entry.branchId;
            if (this.train?.trainNumber)    p.trainNumber    = this.train.trainNumber;
            if (this.train?.operationalDay) p.operationalDay = this.train.operationalDay;
            window.location.href = `train-version.html?${new URLSearchParams(p).toString()}`;
        },

        // ── Formatting / badge helpers (delegated to r2p-utils.js) ───────────
        formatDate:           (d) => formatDate(d),
        formatTimestamp:      (d) => formatTimestamp(d),
        truncateId:           (id) => truncateId(id),
        partnerBadgeClass:    (p)  => partnerBadgeClass(p),
        branchStateBadgeClass:(s)  => branchStateBadgeClass(s),
        actionBadgeClass:     (a)  => actionBadgeClass(a),
        copyToClipboard:      (t)  => copyToClipboard(t)
    };
}

