/**
 * app-details.js — Alpine.js component for details.html (train detail page).
 * Depends on: r2p-utils.js, r2p-api.js, router.js
 */

function detailsApp() {
    return {
        // ── Core state ────────────────────────────────────────────────────────
        trainId: null,
        loading: true,
        error:   null,
        train:   null,

        // ── Raw data ──────────────────────────────────────────────────────────
        allBranches:      [],
        allChangelog:     [],
        allMergeRequests: [],

        // ── Partner token selector ────────────────────────────────────────────
        selectedToken: getSelectedToken(),
        partnerTokens: PARTNER_TOKENS,

        // ── Items per page (shared across both tables) ────────────────────────
        itemsPerPage: 20,

        // ── Collapsible filter bars (collapsed by default, per section) ───────
        mrFiltersExpanded:     false,
        branchFiltersExpanded: false,
        clFiltersExpanded:     false,

        // ── Branch filter / sorter / page ─────────────────────────────────────
        branchFilterStatus: '',
        branchFilterOwner:  '',
        branchSorter:       makeSorter(),
        branchPage:         1,

        // ── Merge request filter / sorter / page ──────────────────────────────
        mrFilterAction: '',
        mrSorter:       makeSorter(),
        mrPage:         1,

        // ── Changelog filter / sorter / page ──────────────────────────────────
        clFilterAction:  '',
        clFilterPartner: '',
        clFilterComment: '',
        clSorter:        makeSorter('timestamp', 'desc'),   // newest first by default
        clPage:          1,

        // ── Active-filter counts (for the per-section Filters count badge) ────
        get mrActiveFilterCount() {
            return this.mrFilterAction ? 1 : 0;
        },
        get branchActiveFilterCount() {
            return (this.branchFilterStatus ? 1 : 0) + (this.branchFilterOwner ? 1 : 0);
        },
        get clActiveFilterCount() {
            return (this.clFilterAction ? 1 : 0)
                 + (this.clFilterPartner ? 1 : 0)
                 + (this.clFilterComment.trim() ? 1 : 0);
        },

        // ── Derived: branch owners dropdown ───────────────────────────────────
        get branchOwners() {
            return [...new Set(this.allBranches.map(b => b.owner))].sort();
        },

        // ── Derived: merge request actions dropdown ───────────────────────────
        get mrActions() {
            return [...new Set(this.allMergeRequests.map(m => m.action))].sort();
        },

        // ── Derived: filtered + sorted merge requests ─────────────────────────
        get filteredMergeRequests() {
            let r = this.allMergeRequests;
            if (this.mrFilterAction)
                r = r.filter(m => m.action === this.mrFilterAction);
            return applySorter(r, this.mrSorter);
        },

        get mrTotalPages() {
            return Math.max(1, Math.ceil(this.filteredMergeRequests.length / this.itemsPerPage));
        },

        get pagedMergeRequests() {
            const start = (this.mrPage - 1) * this.itemsPerPage;
            return this.filteredMergeRequests.slice(start, start + this.itemsPerPage);
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
                const [trainRes, changesRes, mrRes] = await Promise.all([
                    fetchTrainDetail(this.trainId),
                    fetchTrainChanges(this.trainId),
                    fetchMergeRequests(this.trainId)
                ]);

                if (trainRes.status === 404) {
                    this.error = `Train with ID ${this.trainId} was not found.`;
                    return;
                }
                if (!trainRes.ok)
                    throw new Error(`Train API returned HTTP ${trainRes.status} ${trainRes.statusText}`);
                if (!changesRes.ok)
                    throw new Error(`Changes API returned HTTP ${changesRes.status} ${changesRes.statusText}`);
                if (!mrRes.ok)
                    throw new Error(`Merge requests API returned HTTP ${mrRes.status} ${mrRes.statusText}`);

                const trainData   = await trainRes.json();
                const changesData = await changesRes.json();
                const mrData      = await mrRes.json();

                this.train            = this.mapTrain(trainData);
                this.allBranches      = this.mapBranches(trainData.branches ?? []);
                this.allMergeRequests = this.mapMergeRequests(mrData);
                this.allChangelog     = changesData;

                document.title = `R²P Console — Train ${this.train.trainNumber}`;
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        // ── Called when token selector changes ────────────────────────────────
        onTokenChange() {
            setSelectedToken(this.selectedToken);
            this.loading          = true;
            this.error            = null;
            this.train            = null;
            this.allMergeRequests = [];
            this.mrPage           = 1;
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

        // ── Merge request sort helpers ────────────────────────────────────────
        mrSort(col) {
            this.mrSorter.sort(col);
            this.mrPage = 1;
        },

        mrSortIndicator(col) {
            return this.mrSorter.indicator(col);
        },

        mrSortAriaSort(col) {
            return this.mrSorter.ariaSort(col);
        },

        mrPaginationInfo() {
            return paginationInfo(
                this.filteredMergeRequests.length,
                this.mrPage,
                this.itemsPerPage,
                'merge requests'
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

        mapMergeRequests(changes) {
            return changes.map(c => ({
                branchId:  c.branchId,
                branchName: c.branchName,
                partner:   c.partner,
                timestamp: c.timestamp,
                action:    c.action,
                version:   c.version,
                comment:   c.comment
            }));
        },

        // ── Navigation helpers ────────────────────────────────────────────
        /**
         * Opens the train-version page showing the latest version of a branch.
         * @param {{ branchId: string, name: string }} branch
         */
        openBranchVersion(branch) {
            Router.goToBranchVersion(
                this.trainId,
                branch.branchId,
                branch.name,
                this.train?.trainNumber    ?? null,
                this.train?.operationalDay ?? null
            );
        },

        /**
         * Opens the train-version page for a specific version UUID (called from changelog).
         * @param {string} versionId
         * @param {object} entry  changelog entry (has branchName, branchId)
         */
        openVersionViewer(versionId, entry) {
            Router.goToVersion(
                versionId,
                this.trainId,
                entry?.branchName          ?? null,
                entry?.branchId            ?? null,
                this.train?.trainNumber    ?? null,
                this.train?.operationalDay ?? null
            );
        },

        // ── Formatting / badge helpers (delegated to r2p-utils.js) ───────────
        formatDate:           (d) => formatDate(d),
        formatTimestamp:      (d) => formatTimestamp(d),
        truncateId:           (id) => truncateId(id),
        partnerBadgeClass:      (p)  => partnerBadgeClass(p),
        branchStateBadgeClass:  (s)  => branchStateBadgeClass(s),
        actionBadgeClass:       (a)  => actionBadgeClass(a),
        mergeRequestBadgeClass: (a)  => mergeRequestBadgeClass(a),
        copyToClipboard:        (t)  => copyToClipboard(t)
    };
}

