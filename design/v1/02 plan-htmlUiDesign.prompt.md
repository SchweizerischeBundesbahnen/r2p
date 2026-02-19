# Plan: HTML UI Design für R2P Train Overview und Details

Erstellung zweier statischer HTML-Seiten (index.html und details.html) mit modernem, schlichten Design für die Visualisierung von Zugverkehrs-Daten. Die Seiten nutzen reines HTML, CSS und JavaScript ohne externe Frameworks und präsentieren Daten basierend auf dem OpenAPI-Schema und Datenbankmodell.

## Steps

### 1. Create `index.html` - Overview Page
Create `index.html` in `design/` folder with the following sections:
- **Header section**: R2P logo/title with brief introduction explaining R2P (Railway Undertaking Partner communication)
- **System info section**: Display Lead RU (instance owner: SBB = Schweizerische Bundesbahnen) and server version (0.0.12-SNAPSHOT from pom.xml)
- **Statistics section**: Show key metrics:
  - Total train count
  - Total branch count  
  - Total version count
  - Open merge request count
- **Train list table**: Columns include:
  - Train number (from `train_version.train_number`)
  - Operational day (from `train_version.operational_day`, formatted as DD.MM.YYYY)
  - Partners (comma-separated full names from `train_partner` table)
  - Last change (from `train_version.timestamp`, formatted as DD.MM.YYYY HH:MM:SS)
- **Row interaction**: Each table row is clickable and links to `details.html?trainId={uuid}`

### 2. Create `details.html` - Train Details Page
Create `details.html` in `design/` folder with four main sections:

#### Section 1: General Information
Display as card/panel:
- Train ID (UUID from `train_version.train_id`)
- Train number (from `train_version.train_number`)
- Operational day (formatted DD.MM.YYYY)
- Partners (full names, comma-separated from `train_partner` table)

#### Section 2: Merge Requests
Table showing open merge requests (future feature - show placeholder):
- Columns: ID, Creator, Creation Date (DD.MM.YYYY HH:MM:SS), Comment
- Sorted by creation date descending
- Note: Display "No merge requests available" message as this feature is not yet implemented

#### Section 3: Branches
Table displaying all branches:
- Columns: Branch ID (UUID), Name, Owner (full name), Creation Date (DD.MM.YYYY HH:MM:SS), State
- State values: open, merged, deleted (from `BranchState` enum in OpenAPI spec)
- Sorted by creation date descending
- Data from `branch` table

#### Section 4: Changelog
Table showing change history:
- Columns: Action, Timestamp (DD.MM.YYYY HH:MM:SS), Branch Name, Version ID, Partner, Comment
- Action values: created, updated, merged, deleted, shared, unshared (from `Action` enum in OpenAPI spec)
- Sorted by timestamp descending
- Data from `changelog` table

### 3. Create `style.css` - Shared Stylesheet
Create `style.css` in `design/` folder with:

#### Layout & Structure
- Clean, modern design with card-based sections
- Proper spacing and padding
- Professional color scheme suitable for railway/infrastructure context
- Swiss design principles (clarity, precision, functionality)

#### Typography
- Clear hierarchy with appropriate font sizes
- Readable font family (system fonts)
- Proper line height and letter spacing

#### Tables
- Clean table styling with headers
- Striped rows for better readability
- Hover effects on clickable rows
- Proper column alignment
- Responsive column widths

#### Color Scheme
- Primary color: Professional blue/gray palette
- Accent colors for status indicators:
  - Green for "open" state
  - Blue for "merged" state  
  - Gray for "deleted" state
- Neutral background colors
- High contrast for accessibility

#### Components
- Card/panel styling for information sections
- Button styling (if needed for navigation)
- Status badge styling for branch states
- Action badge styling for changelog actions

### 4. Populate with Static Sample Data
Create realistic sample data demonstrating:

#### Train Data (3-4 sample trains)
- Different train numbers (e.g., IC 1234, RE 5678, S 9)
- Various operational days (recent dates in 2026)
- Different partner combinations:
  - Train 1: SBB only (not shared)
  - Train 2: SBB, BLS shared
  - Train 3: SBB, BLS, SOB shared
- Various last change timestamps

#### Branch Data (per train)
- Main branch (owned by SBB, state: open)
- Partner branches (owned by BLS/SOB, various states)
- Demonstrate all three states: open, merged, deleted

#### Changelog Data (per train)
- All action types: created, updated, merged, deleted, shared, unshared
- Multiple entries showing evolution over time
- Different partners performing actions
- Meaningful comments describing changes

#### Statistics
Calculate from sample data:
- Total trains: 3-4
- Total branches: 8-12 (including all branches across trains)
- Total versions: 15-20 (showing multiple versions per branch)
- Open merge requests: 0 (not implemented yet)

### 5. Add JavaScript Functionality
Create inline or separate `script.js` with:

#### Date Formatting
- Function to format ISO dates to Swiss format (DD.MM.YYYY)
- Function to format ISO timestamps to Swiss format (DD.MM.YYYY HH:MM:SS)
- Apply to all date/timestamp fields on page load

#### Partner Name Expansion
- Mapping object for abbreviations to full names:
  - SBB → Schweizerische Bundesbahnen
  - BLS → BLS AG
  - SOB → Südostbahn AG
- Function to expand partner names in display

#### Navigation
- Click handler for train table rows on index.html
- Extract train ID and navigate to details.html with query parameter
- "Back to Overview" link/button on details.html
- Parse URL query parameters on details.html to show relevant train data

#### Dynamic Content
- On details.html, read trainId from URL parameter
- Display corresponding train data from static dataset
- If no match found, show appropriate message

## Data Sources

### OpenAPI Schemas (r2p-spec.yaml, train.yaml)
- `TrainListElement`: trainId, mainTimestamp, partners, branches
- `Branch`: branchId, branchName, business (trainNumber, operationalDay), technical (owner, version, branchState, timestamp, comment)
- `Change`: action, partner, affectedPartner, trainId, branchId, branchName, version, timestamp, comment
- `BranchState` enum: open, merged, deleted
- `Action` enum: created, updated, merged, deleted, shared, unshared
- `PartnerToken` enum: SBB, BLS, SOB

### Database Schema (V1__create.sql)
- `train_version` table: version, train_id, train_number, operational_day, branch_id, comment, timestamp, payload, head
- `branch` table: branch_id, train_id, name, owner, state
- `train_partner` table: train_id, partner
- `changelog` table: id, action, partner, affected_partner, train_id, branch_id, branch_name, version, timestamp, comment

## Technical Requirements

### Constraints
- No external libraries or frameworks
- Pure HTML, CSS, and JavaScript only
- Static data (no API/database connections)
- Desktop browser optimized (no mobile responsiveness required)
- English language UI
- Swiss date/time formatting (DD.MM.YYYY HH:MM:SS)
- Full partner names (not abbreviations)

### Browser Compatibility
- Modern browsers (Chrome, Firefox, Edge, Safari)
- ES6+ JavaScript features allowed
- CSS Grid/Flexbox for layouts

## Implementation Notes

1. **File Structure**:
   ```
   design/
   ├── index.html
   ├── details.html
   ├── style.css
   └── plan-htmlUiDesign.prompt.md
   ```

2. **Sample Data**: Embed sample data as JavaScript objects in the HTML files or create a separate `data.js` file

3. **Merge Requests**: Show placeholder message "Merge requests are not yet implemented. This feature will be available in a future release."

4. **Navigation**: Include "Back to Overview" button on details.html for better UX

5. **URL Parameters**: Use `URLSearchParams` API to read trainId from query string

6. **Status Indicators**: Use colored badges or labels for branch states and changelog actions

7. **Table Sorting**: Data is pre-sorted in the static dataset (descending by date)

8. **Accessibility**: Include proper semantic HTML, alt texts, and ARIA labels where appropriate

## Validation Criteria

- [ ] index.html displays all required sections with sample data
- [ ] details.html shows all four sections correctly
- [ ] Swiss date/time format applied consistently
- [ ] Partner names shown in full (not abbreviations)
- [ ] Table rows on index.html are clickable
- [ ] Navigation from index to details works
- [ ] All branch states visible in sample data
- [ ] All changelog actions demonstrated in sample data
- [ ] Clean, modern, professional design
- [ ] No external dependencies used
- [ ] Code is well-structured and commented

