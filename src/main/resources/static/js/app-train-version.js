/**
 * app-train-version.js — Alpine.js component for train-version.html.
 * Displays the train JSON for a specific version or the latest version of a branch.
 * Depends on: r2p-utils.js, r2p-api.js
 *
 * URL parameters (one of the following combinations is required):
 *   ?version=<uuid>                           → show a specific version
 *   ?trainId=<uuid>&branchId=<uuid>           → show latest version of a branch
 *   ?trainId=<uuid>&branchId=<uuid>&branchName=<name>  → same, with display name
 */

function trainVersionApp() {
    return {
        // ── State ─────────────────────────────────────────────────────────────
        loading:     true,
        error:       null,
        trainData:   null,   // raw JSON object from API
        jsonString:  '',     // pretty-printed JSON for display

        // ── Context info (for the header) ─────────────────────────────────────
        versionId:      null,
        trainId:        null,
        branchId:       null,
        branchName:     null,
        trainNumber:    null,
        operationalDay: null,
        backUrl:        'index.html',   // fallback

        // ── Search ────────────────────────────────────────────────────────────
        searchQuery:    '',
        searchResults:  [],   // [{path, label, value, nodeEl}]
        searchIndex:    -1,
        searchStatus:   '',

        // ── Lifecycle ─────────────────────────────────────────────────────────
        async init() {
            const params  = new URLSearchParams(window.location.search);
            this.versionId      = params.get('version');
            this.trainId        = params.get('trainId');
            this.branchId       = params.get('branchId');
            this.branchName     = params.get('branchName') ?? null;
            this.trainNumber    = params.get('trainNumber') ?? null;
            this.operationalDay = params.get('operationalDay') ?? null;

            // Build "back" URL restoring the details page for this train
            if (this.trainId) {
                this.backUrl = `details.html?trainId=${encodeURIComponent(this.trainId)}`;
            }

            try {
                if (this.versionId) {
                    await this._loadVersion(this.versionId);
                } else if (this.trainId && this.branchId) {
                    await this._loadBranchLatest(this.trainId, this.branchId);
                } else {
                    this.error = 'Missing parameters. Provide either version= or trainId= and branchId=.';
                }
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
                // Render the JSON viewer after Alpine updates the DOM
                if (this.trainData) {
                    this.$nextTick(() => this.renderJson());
                }
            }
        },

        // ── Private loaders ───────────────────────────────────────────────────
        async _loadVersion(versionId) {
            const res = await fetchTrainVersion(versionId);
            if (res.status === 404) throw new Error(`Version ${versionId} not found.`);
            if (res.status === 403) throw new Error('Not authorized to view this version.');
            if (!res.ok) throw new Error(`API returned HTTP ${res.status} ${res.statusText}`);
            this.trainData  = await res.json();
            this.jsonString = JSON.stringify(this.trainData, null, 2);
            document.title  = `Version ${this.versionId.substring(0, 8)}… - R2P`;
        },

        async _loadBranchLatest(trainId, branchId) {
            // Step 1: fetch branch to get latest version ID
            const branchRes = await fetchTrainBranch(trainId, branchId);
            if (branchRes.status === 404) throw new Error(`Branch ${branchId} not found.`);
            if (branchRes.status === 403) throw new Error('Not authorized to view this branch.');
            if (!branchRes.ok) throw new Error(`API returned HTTP ${branchRes.status} ${branchRes.statusText}`);
            const branch = await branchRes.json();

            if (!this.branchName)     this.branchName     = branch.branchName ?? null;
            if (!this.trainNumber)    this.trainNumber    = branch.business?.trainNumber ?? null;
            if (!this.operationalDay) this.operationalDay = branch.business?.operationalDay ?? null;
            const latestVersion = branch.technical?.version;
            if (!latestVersion) throw new Error('Branch has no version yet.');

            this.versionId = latestVersion;

            // Step 2: fetch the actual train data for that version
            await this._loadVersion(latestVersion);
            document.title = `Branch ${this.branchName ?? branchId.substring(0, 8)} - R2P`;
        },

        // ── Computed title helpers ────────────────────────────────────────────
        get pageTitle() {
            const train = [this.trainNumber, this.operationalDay ? formatDate(this.operationalDay) : null]
                .filter(Boolean).join(' · ');
            if (this.branchName) return train ? `${this.branchName}  —  ${train}` : `Branch: ${this.branchName}`;
            if (this.versionId)  return train ? `Version ${this.versionId.substring(0, 8)}…  —  ${train}`
                                              : `Version: ${this.versionId.substring(0, 8)}…`;
            return 'Train Version';
        },

        get pageSubtitle() {
            if (this.branchName && this.versionId) {
                return `Latest version ID: ${this.versionId.substring(0, 8)}…`;
            }
            return '';
        },

        // ── JSON Search ───────────────────────────────────────────────────────
        /**
         * Performs a text search across all keys and values in the JSON object.
         * Builds a flat list of matching paths, then highlights the first match.
         */
        doSearch() {
            this.searchResults = [];
            this.searchIndex   = -1;
            this.searchStatus  = '';

            const q = this.searchQuery.trim().toLowerCase();
            if (!q || !this.trainData) return;

            this._collectMatches(this.trainData, [], q);

            if (this.searchResults.length === 0) {
                this.searchStatus = 'No matches found.';
                return;
            }
            this.searchStatus = `1 / ${this.searchResults.length} match${this.searchResults.length > 1 ? 'es' : ''}`;
            this.searchIndex  = 0;
            this._scrollToMatch(0);
        },

        searchNext() {
            if (this.searchResults.length === 0) return;
            this.searchIndex = (this.searchIndex + 1) % this.searchResults.length;
            this.searchStatus = `${this.searchIndex + 1} / ${this.searchResults.length} matches`;
            this._scrollToMatch(this.searchIndex);
        },

        searchPrev() {
            if (this.searchResults.length === 0) return;
            this.searchIndex = (this.searchIndex - 1 + this.searchResults.length) % this.searchResults.length;
            this.searchStatus = `${this.searchIndex + 1} / ${this.searchResults.length} matches`;
            this._scrollToMatch(this.searchIndex);
        },

        /**
         * Recursively walks the JSON object and collects paths that match query.
         * @param {any} obj
         * @param {string[]} path
         * @param {string} q   lower-cased search string
         */
        _collectMatches(obj, path, q) {
            if (obj === null || obj === undefined) return;

            if (typeof obj !== 'object') {
                // Primitive: check the value
                const strVal = String(obj).toLowerCase();
                if (strVal.includes(q)) {
                    this.searchResults.push({ path: [...path], type: 'value' });
                }
                return;
            }

            if (Array.isArray(obj)) {
                obj.forEach((item, i) => this._collectMatches(item, [...path, String(i)], q));
            } else {
                Object.entries(obj).forEach(([key, value]) => {
                    // Match against keys
                    if (key.toLowerCase().includes(q)) {
                        this.searchResults.push({ path: [...path, key], type: 'key' });
                    }
                    this._collectMatches(value, [...path, key], q);
                });
            }
        },

        /**
         * Expands the pretty-json tree to make the matched node visible, then scrolls to it.
         * The pretty-json custom element uses nested `<pretty-json key="…">` elements.
         * We walk the shadow DOM path to expand all ancestors and find the target row.
         * @param {number} idx
         */
        _scrollToMatch(idx) {
            if (idx < 0 || idx >= this.searchResults.length) return;
            const match = this.searchResults[idx];

            // Clear previous shadow-DOM primitive row highlight
            if (this._prevHighlightRow) {
                this._prevHighlightRow.style.backgroundColor = '';
                this._prevHighlightRow.style.outline = '';
                this._prevHighlightRow.style.borderRadius = '';
                this._prevHighlightRow = null;
            }

            // Remove previous light-DOM highlight classes
            document.querySelectorAll('pretty-json.json-search-highlight').forEach(el => {
                el.classList.remove('json-search-highlight');
            });

            const viewer = document.getElementById('json-viewer');
            if (!viewer) return;

            const root = viewer.querySelector('pretty-json');
            if (!root) return;

            this._expandAndHighlight(root, match.path, match.type);
        },

        /**
         * Walks the pretty-json shadow DOM tree along the given path,
         * expanding ONLY the ancestor nodes on the path to the match
         * (siblings stay collapsed), then highlights the final match.
         *
         * Strategy: pretty-json renders children with expand=(parent_expand - 1).
         * To expose only the path root→target we set each ancestor's expand to
         * exactly the remaining depth needed:
         *   root        → expand = path.length
         *   path[0] child → expand = path.length - 1
         *   …
         *   direct parent of target → expand = 1   (shows target but collapsed)
         */
        _expandAndHighlight(rootEl, path, matchType) {
            /**
             * Set expand on el to at least `needed`, but never shrink it below
             * its current value so that a previously-opened subtree isn't collapsed.
             */
            function setExpand(el, needed) {
                const current = parseInt(el.getAttribute('expand') ?? '0', 10);
                if (isNaN(current) || current < needed) {
                    el.setAttribute('expand', String(needed));
                }
            }

            /** Search only the direct (non-nested) pretty-json children inside a shadow root. */
            function findDirectChildByKey(el, key) {
                if (!el.shadowRoot) return null;
                // querySelectorAll would also return deeply-nested children; we only
                // want the immediate pretty-json children of this shadow root.
                for (const child of el.shadowRoot.children) {
                    // The container div holds pretty-json elements as direct children
                    if (child.tagName === 'DIV') {
                        for (const grandchild of child.children) {
                            if (grandchild.tagName === 'PRETTY-JSON' &&
                                grandchild.getAttribute('key') === key) {
                                return grandchild;
                            }
                        }
                    }
                    if (child.tagName === 'PRETTY-JSON' && child.getAttribute('key') === key) {
                        return child;
                    }
                }
                return null;
            }

            /** Find a primitive .row inside a shadow root whose .key-name matches key. */
            function findPrimitiveRow(el, key) {
                if (!el.shadowRoot) return null;
                // Walk only the top-level container's direct .row children
                const container = el.shadowRoot.querySelector('.container');
                if (!container) return null;
                for (const row of container.children) {
                    if (!row.classList.contains('row')) continue;
                    const keySpan = row.querySelector('.key-name');
                    if (keySpan && keySpan.textContent === JSON.stringify(key)) return row;
                }
                return null;
            }

            // The root needs to be expanded deep enough to show the full path.
            // path.length gives the number of steps; root needs expand >= path.length.
            setExpand(rootEl, path.length);

            // Walk down the path, expanding each step with only as much depth as needed.
            let current = rootEl;
            for (let i = 0; i < path.length - 1; i++) {
                const seg = path[i];
                // After setExpand(current, path.length - i), current's shadow DOM is rendered.
                // We must wait for the render; pretty-json renders synchronously on setAttribute,
                // so we can look up the child immediately.
                const child = findDirectChildByKey(current, seg);
                if (!child) {
                    // Array indices appear as plain .row entries, not nested pretty-json.
                    // Fall back to a broader search one level deeper.
                    const fallback = current.shadowRoot
                        ? current.shadowRoot.querySelector(`pretty-json[key="${CSS.escape(seg)}"]`)
                        : null;
                    if (!fallback) break;
                    setExpand(fallback, path.length - i - 1);
                    current = fallback;
                    continue;
                }
                // Expand child only enough to show the next step
                setExpand(child, path.length - i - 1);
                current = child;
            }

            // Highlight the final segment ──────────────────────────────────────
            const lastKey = path[path.length - 1];

            // Case A: the target is a nested object/array → a <pretty-json key=lastKey>
            const childEl = findDirectChildByKey(current, lastKey)
                ?? (current.shadowRoot
                    ? current.shadowRoot.querySelector(`pretty-json[key="${CSS.escape(lastKey)}"]`)
                    : null);
            if (childEl) {
                // Expand just enough to show the object/array header (expand=1)
                setExpand(childEl, 1);
                childEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                childEl.classList.add('json-search-highlight');
                return;
            }

            // Case B: the target is a primitive .row → highlight via shadow-DOM style
            const rowEl = findPrimitiveRow(current, lastKey);
            if (rowEl) {
                current.scrollIntoView({ behavior: 'smooth', block: 'center' });
                rowEl.style.backgroundColor = '#fff3cd';
                rowEl.style.outline = '2px solid #ffc107';
                rowEl.style.borderRadius = '3px';
                this._prevHighlightRow = rowEl;
                return;
            }

            // Fallback: highlight the parent element
            current.scrollIntoView({ behavior: 'smooth', block: 'center' });
            current.classList.add('json-search-highlight');
        },

        // ── Expand / Collapse all ─────────────────────────────────────────────

        /**
         * Programmatically creates and inserts a <pretty-json> element into #json-viewer.
         * We must set textContent BEFORE appending to the DOM so that connectedCallback
         * (which calls JSON.parse(this.textContent)) sees the data.
         */
        renderJson() {
            const viewer = document.getElementById('json-viewer');
            if (!viewer || !this.jsonString) return;
            viewer.innerHTML = '';
            const el = document.createElement('pretty-json');
            el.setAttribute('expand', '3');
            el.setAttribute('truncate-string', '500');
            el.textContent = this.jsonString;  // set BEFORE appending
            viewer.appendChild(el);
        },

        expandAll() {
            const viewer = document.getElementById('json-viewer');
            if (!viewer) return;
            viewer.querySelectorAll('pretty-json').forEach(el => {
                el.setAttribute('expand', '999');
            });
        },

        collapseAll() {
            const viewer = document.getElementById('json-viewer');
            if (!viewer) return;
            // Only collapse top-level element; children will follow
            const root = viewer.querySelector('pretty-json');
            if (root) root.setAttribute('expand', '1');
        },

        // ── Formatting helpers ────────────────────────────────────────────────
        truncateId:      (id) => truncateId(id),
        copyToClipboard: (t)  => copyToClipboard(t),
        formatTimestamp: (d)  => formatTimestamp(d),
        formatDate:      (d)  => formatDate(d),
    };
}










