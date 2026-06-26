/**
 * app-index.js — Alpine.js component for index.html (train overview page).
 * Depends on: r2p-utils.js, r2p-api.js
 */

function trainApp() {
    return {
        // ── Raw data from server ──────────────────────────────────────────────
        allTrains: [],
        loading:   true,
        error:     null,

        // ── Statistics data from server ───────────────────────────────────────
        stats: [],          // array from /internal/stats; first element is totals (partner === null)
        statsError: null,
        instanceOwnerName: '…',

        // ── Timetable year filter (mandatory; drives date range) ──────────────
        filterTimetableYear: String(getCurrentTimetableYear()),
        timetableYearOptions: getTimetableYearOptions(),

        // ── Server-side date filters (trigger new fetch on change) ────────────
        filterDateFrom: '',
        filterDateTo:   '',

        // ── Client-side filters ───────────────────────────────────────────────
        filterTrainNumber:  '',
        filterPartner:      '',
        filterOpenBranches: false,
        filterOpenMRs:      false,

        // ── Collapsible filter bar (collapsed by default) ─────────────────────
        filtersExpanded: false,

        // ── Statistics partner selection (drives stat cards) ──────────────────
        selectedStatPartner: '',   // '' = show totals row

        // ── Derived partner list for the dropdown ─────────────────────────────
        availablePartners: [],

        // ── Partner token selector ────────────────────────────────────────────
        selectedToken: getSelectedToken(),
        partnerTokens: PARTNER_TOKENS,

        // ── Sorting (single sorter for the trains table) ──────────────────────
        sorter: makeSorter(),

        // ── Pagination ────────────────────────────────────────────────────────
        currentPage:  1,
        itemsPerPage: 20,

        // ── Derived: bounds of the selected timetable year ────────────────────
        get timetableYearBounds() {
            return getTimetableYearBounds(Number(this.filterTimetableYear));
        },

        // ── Derived: number of active client-side filters (for the count badge) ─
        get activeFilterCount() {
            let count = 0;
            if (this.filterTrainNumber.trim()) count++;
            if (this.filterPartner) count++;
            if (this.filterOpenBranches) count++;
            if (this.filterOpenMRs) count++;
            return count;
        },

        // ── Derived: stats row matching the selected stat partner ─────────────
        get activeStats() {
            if (!this.selectedStatPartner)
                return this.stats.find(s => s.partner === null) ?? {};
            return this.stats.find(s => s.partner === this.selectedStatPartner) ?? {};
        },

        // ── Derived: partner list for the stats dropdown (excluding totals) ───
        get statsPartners() {
            return this.stats.filter(s => s.partner !== null);
        },

        // ── Derived: filtered + sorted + paged ───────────────────────────────
        get filteredTrains() {
            let result = this.allTrains;

            if (this.filterTrainNumber.trim()) {
                const q = this.filterTrainNumber.trim().toLowerCase();
                result = result.filter(t => t.trainNumber.toLowerCase().includes(q));
            }

            if (this.filterPartner) {
                result = result.filter(t =>
                    t.leadRU === this.filterPartner ||
                    t.otherPartners.includes(this.filterPartner)
                );
            }

            if (this.filterOpenBranches) {
                result = result.filter(t => t.openBranches > 0);
            }

            if (this.filterOpenMRs) {
                result = result.filter(t => t.openMergeRequests > 0);
            }

            return applySorter(result, this.sorter);
        },

        get totalPages() {
            return Math.max(1, Math.ceil(this.filteredTrains.length / this.itemsPerPage));
        },

        get pagedTrains() {
            const start = (this.currentPage - 1) * this.itemsPerPage;
            return this.filteredTrains.slice(start, start + this.itemsPerPage);
        },

        // ── Lifecycle ─────────────────────────────────────────────────────────
        async init() {
            // Initialise date range to the current timetable year bounds
            const bounds = this.timetableYearBounds;
            this.filterDateFrom = bounds.start;
            this.filterDateTo   = bounds.end;
            await Promise.all([this.fetchTrains(), this.fetchStatsData(), this.fetchInstanceOwnerData()]);
        },

        // ── Server fetch (called on init and when date filters change) ─────────
        async fetchTrains() {
            this.loading = true;
            this.error   = null;
            try {
                const bounds = this.timetableYearBounds;
                const response = await fetchTrainList({
                    operationalDayFrom: this.filterDateFrom || bounds.start,
                    operationalDayTo:   this.filterDateTo   || bounds.end
                });
                if (!response.ok) throw new Error(`HTTP ${response.status} ${response.statusText}`);

                const data = await response.json();
                this.allTrains = data.map(t => this.mapTrain(t));
                this.rebuildPartnerList();
                this.currentPage = 1;
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        // ── Stats fetch (called on init) ───────────────────────────────────────
        async fetchStatsData() {
            this.statsError = null;
            try {
                const response = await fetchStats();
                if (!response.ok) throw new Error(`HTTP ${response.status} ${response.statusText}`);
                this.stats = await response.json();
            } catch (e) {
                this.statsError = e.message;
            }
        },

        // ── Instance owner fetch (called on init) ──────────────────────────────
        async fetchInstanceOwnerData() {
            try {
                const response = await fetchInstanceOwner();
                if (!response.ok) throw new Error(`HTTP ${response.status} ${response.statusText}`);
                const data = await response.json();
                this.instanceOwnerName = data.name ?? '…';
            } catch (e) {
                // non-fatal: header subtitle stays '…'
            }
        },

        // ── Called when token selector changes ────────────────────────────────
        onTokenChange() {
            setSelectedToken(this.selectedToken);
            this.fetchTrains();
            this.fetchStatsData();
        },

        // ── Called when timetable year selector changes ────────────────────────
        onTimetableYearChange() {
            const bounds = this.timetableYearBounds;
            this.filterDateFrom = bounds.start;
            this.filterDateTo   = bounds.end;
            this.fetchTrains();
        },

        // ── Called by client-side filter controls ──────────────────────────────
        applyClientFilters() {
            this.currentPage = 1;
        },

        // ── Rebuild the partner dropdown from current allTrains ────────────────
        rebuildPartnerList() {
            const partners = new Set();
            this.allTrains.forEach(t => {
                partners.add(t.leadRU);
                t.otherPartners.forEach(p => partners.add(p));
            });
            this.availablePartners = Array.from(partners).sort();
        },

        // ── Sorting (delegated to sorter object) ───────────────────────────────
        sort(column) {
            this.sorter.sort(column);
            this.currentPage = 1;
        },

        sortIndicator(column) {
            return this.sorter.indicator(column);
        },

        sortAriaSort(column) {
            return this.sorter.ariaSort(column);
        },

        // ── Pagination ────────────────────────────────────────────────────────
        goToPage(page) {
            if (page < 1 || page > this.totalPages) return;
            this.currentPage = page;
        },

        paginationInfo() {
            return paginationInfo(
                this.filteredTrains.length,
                this.currentPage,
                this.itemsPerPage,
                'trains'
            );
        },

        // ── Data mapping ──────────────────────────────────────────────────────
        mapTrain(t) {
            const base = mapTrainBase(t);
            const openBranches = (t.branches ?? []).filter(
                b => b.technical && b.technical.branchState === 'open' && !b.technical.mainBranch
            ).length;
            const openMergeRequests = (t.branches ?? []).filter(
                b => b.technical && b.technical.branchState === 'merge_requested'
            ).length;
            return {
                ...base,
                openBranches,
                openMergeRequests,
                lastChange: t.mainTimestamp ?? null
            };
        },

        // ── Formatting helpers (delegated to r2p-utils.js) ────────────────────
        formatDate:        (d)  => formatDate(d),
        formatTimestamp:   (d)  => formatTimestamp(d),
        partnerBadgeClass: (p)  => partnerBadgeClass(p)
    };
}

