// Extended Sample Data for R2P UI v2
// This file contains realistic test data for pagination, filtering, and sorting

const sampleData = {
    trains: []
};

// Helper function to generate train data
function generateTrains() {
    const partners = ['SBB', 'BLS', 'SOB', 'DB', 'ÖBB', 'SNCF', 'Trenitalia'];
    const trainTypes = ['IC', 'RE', 'S', 'IR', 'EC', 'RJ'];
    const trainNumbers = [];

    // Generate 50 trains
    for (let i = 1; i <= 50; i++) {
        const trainType = trainTypes[Math.floor(Math.random() * trainTypes.length)];
        const trainNumber = 1000 + Math.floor(Math.random() * 9000);
        const trainId = `550e8400-e29b-41d4-a716-4466554400${String(i).padStart(2, '0')}`;

        // Operational day: distribute between today-5 and today+14
        const daysOffset = Math.floor(Math.random() * 20) - 5;
        const operationalDate = new Date(2026, 1, 13 + daysOffset); // February 13, 2026 + offset

        // Lead RU: mostly SBB, but some others
        const leadRU = i <= 40 ? 'SBB' : partners[Math.floor(Math.random() * 3) + 1];

        // Other partners: 0-3 additional partners
        const numPartners = Math.floor(Math.random() * 4);
        const otherPartners = [];
        for (let j = 0; j < numPartners; j++) {
            const partner = partners[Math.floor(Math.random() * partners.length)];
            if (partner !== leadRU && !otherPartners.includes(partner)) {
                otherPartners.push(partner);
            }
        }

        const allPartners = [leadRU, ...otherPartners];

        // Last change: within last 10 days
        const lastChangeDays = Math.floor(Math.random() * 10);
        const lastChangeHours = Math.floor(Math.random() * 24);
        const lastChangeDate = new Date(2026, 1, 13 - lastChangeDays, lastChangeHours, Math.floor(Math.random() * 60));

        // Branches
        const numBranches = 1 + Math.floor(Math.random() * (i === 5 ? 25 : 5)); // Train 5 has many branches
        const branches = [];
        const changelog = [];

        // Main branch always exists
        const mainBranchId = `b50e8400-e29b-41d4-a716-4466554400${String(i * 10 + 1).padStart(2, '0')}`;
        const mainBranchCreated = new Date(operationalDate.getTime() - (10 * 24 * 60 * 60 * 1000));
        branches.push({
            branchId: mainBranchId,
            name: `${leadRU}-main`,
            owner: leadRU,
            state: 'open',
            createdAt: mainBranchCreated.toISOString()
        });

        // Changelog: initial creation
        changelog.push({
            action: 'created',
            timestamp: mainBranchCreated.toISOString(),
            branchName: `${leadRU}-main`,
            version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
            partner: leadRU,
            comment: 'Initial train creation'
        });

        // Add partner sharing events
        otherPartners.forEach((partner, idx) => {
            const shareDate = new Date(mainBranchCreated.getTime() + ((idx + 1) * 3600000));
            changelog.push({
                action: 'shared',
                timestamp: shareDate.toISOString(),
                branchName: `${leadRU}-main`,
                version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
                partner: leadRU,
                comment: `Train shared with ${partner}`,
                affectedPartner: partner
            });
        });

        // Additional branches
        for (let b = 2; b <= numBranches; b++) {
            const branchOwner = otherPartners.length > 0 && b > 2
                ? otherPartners[Math.floor(Math.random() * otherPartners.length)]
                : leadRU;

            const states = ['open', 'merged', 'deleted'];
            const stateWeights = [0.6, 0.3, 0.1]; // 60% open, 30% merged, 10% deleted
            const rand = Math.random();
            let state = 'open';
            if (rand < stateWeights[2]) state = 'deleted';
            else if (rand < stateWeights[1] + stateWeights[2]) state = 'merged';

            const branchId = `b50e8400-e29b-41d4-a716-4466554400${String(i * 10 + b).padStart(2, '0')}`;
            const branchCreated = new Date(mainBranchCreated.getTime() + (b * 24 * 60 * 60 * 1000));

            const branchNames = ['proposal', 'optimization', 'update', 'changes', 'integration', 'revision'];
            const branchName = `${branchOwner}-${branchNames[Math.floor(Math.random() * branchNames.length)]}-${b}`;

            branches.push({
                branchId: branchId,
                name: branchName,
                owner: branchOwner,
                state: state,
                createdAt: branchCreated.toISOString()
            });

            // Changelog: branch creation
            changelog.push({
                action: 'created',
                timestamp: branchCreated.toISOString(),
                branchName: branchName,
                version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
                partner: branchOwner,
                comment: `Created branch for ${branchNames[Math.floor(Math.random() * branchNames.length)]}`
            });

            // Add 1-3 updates to branch
            const numUpdates = Math.floor(Math.random() * 3) + 1;
            for (let u = 0; u < numUpdates; u++) {
                const updateDate = new Date(branchCreated.getTime() + ((u + 1) * 12 * 60 * 60 * 1000));
                changelog.push({
                    action: 'updated',
                    timestamp: updateDate.toISOString(),
                    branchName: branchName,
                    version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
                    partner: branchOwner,
                    comment: `Updated ${['timings', 'route', 'rolling stock', 'capacity', 'stops'][Math.floor(Math.random() * 5)]}`
                });
            }

            // Merge or delete
            if (state === 'merged') {
                const mergeDate = new Date(branchCreated.getTime() + ((numUpdates + 1) * 24 * 60 * 60 * 1000));
                changelog.push({
                    action: 'merged',
                    timestamp: mergeDate.toISOString(),
                    branchName: branchName,
                    version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
                    partner: leadRU,
                    comment: `Merged ${branchName} into main`
                });
            } else if (state === 'deleted') {
                const deleteDate = new Date(branchCreated.getTime() + ((numUpdates + 1) * 24 * 60 * 60 * 1000));
                changelog.push({
                    action: 'deleted',
                    timestamp: deleteDate.toISOString(),
                    branchName: branchName,
                    version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
                    partner: branchOwner,
                    comment: 'Branch withdrawn'
                });
            }
        }

        // Add some random unshare events (10% chance)
        if (Math.random() < 0.1 && otherPartners.length > 0) {
            const unsharePartner = otherPartners[Math.floor(Math.random() * otherPartners.length)];
            const unshareDate = new Date(lastChangeDate.getTime() - (24 * 60 * 60 * 1000));
            changelog.push({
                action: 'unshared',
                timestamp: unshareDate.toISOString(),
                branchName: `${leadRU}-main`,
                version: `v50e8400-e29b-41d4-a716-4466554400${String(changelog.length + 1).padStart(2, '0')}`,
                partner: leadRU,
                comment: `Removed access for ${unsharePartner}`,
                affectedPartner: unsharePartner
            });
        }

        // Open merge requests (5% chance)
        const openMergeRequests = Math.random() < 0.05 ? Math.floor(Math.random() * 3) + 1 : 0;

        // Open branches count
        const openBranches = branches.filter(b => b.state === 'open').length;

        sampleData.trains.push({
            trainId: trainId,
            trainNumber: `${trainType} ${trainNumber}`,
            operationalDay: operationalDate.toISOString().split('T')[0],
            leadRU: leadRU,
            partners: allPartners,
            lastChange: lastChangeDate.toISOString(),
            openMergeRequests: openMergeRequests,
            openBranches: openBranches,
            branches: branches,
            changelog: changelog.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
        });
    }
}

// Generate the data
generateTrains();

// Export for use in HTML files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { sampleData };
}

