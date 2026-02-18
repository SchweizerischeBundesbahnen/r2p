# R2P HTML UI Design - Static Prototype

This folder contains a static HTML prototype for the R2P (Railway Undertaking Partner Communication) system. The prototype demonstrates the UI design for train overview and detail pages.

## Files

- **index.html** - Train overview page with statistics and train list
- **details.html** - Train details page with branches and changelog
- **style.css** - Shared stylesheet for both pages
- **plan-htmlUiDesign.prompt.md** - Detailed implementation plan
- **US Design.md** - Original user story requirements

## Features

### Overview Page (index.html)
- Header with R2P branding and description
- System information (Lead RU and server version)
- Statistics cards showing:
  - Total trains (4)
  - Total branches (10)
  - Total versions (18)
  - Open merge requests (0)
- Interactive train table with:
  - Train number
  - Operational day (Swiss format: DD.MM.YYYY)
  - Partners (full names)
  - Last change timestamp (Swiss format: DD.MM.YYYY HH:MM:SS)
- Clickable rows that navigate to details page

### Details Page (details.html)
- Back navigation to overview
- Four main sections:

#### 1. General Information
- Train ID (UUID)
- Train number
- Operational day
- Partners (full names, comma-separated)

#### 2. Merge Requests
- Placeholder message indicating feature not yet implemented

#### 3. Branches
- Table showing all branches with:
  - Branch ID (UUID)
  - Name
  - Owner (full name)
  - Creation date
  - State (open/merged/deleted with colored badges)

#### 4. Changelog
- Complete change history with:
  - Action type (created/updated/merged/deleted/shared/unshared)
  - Timestamp (Swiss format)
  - Branch name
  - Version ID (shortened UUID)
  - Partner (full name)
  - Comment

## How to Use

1. Open `index.html` in a web browser
2. Browse the train list and statistics
3. Click on any train row to view details
4. Use the "Back to Overview" link to return to the main page

## Sample Data

The prototype includes 4 sample trains demonstrating various scenarios:

1. **IC 1234** - Single partner (SBB only), 1 branch, 2 versions
2. **RE 5678** - Two partners (SBB, BLS), 3 branches (1 merged), 6 changelog entries
3. **S 9** - Three partners (SBB, BLS, SOB), 4 branches (1 merged, 1 deleted), 9 changelog entries
4. **IR 2345** - Two partners (SBB, SOB), 2 branches (1 merged), 5 changelog entries

## Design Features

- **Swiss Date/Time Formatting**: All dates use DD.MM.YYYY format, timestamps use DD.MM.YYYY HH:MM:SS
- **Full Partner Names**: Partner abbreviations are expanded (SBB → Schweizerische Bundesbahnen, etc.)
- **Clean, Modern Design**: Professional color scheme suitable for railway/infrastructure context
- **Status Indicators**: Color-coded badges for branch states and changelog actions
- **No External Dependencies**: Pure HTML, CSS, and JavaScript only
- **Accessible**: Semantic HTML, proper headings, and readable contrast

## Technical Details

- **No frameworks**: Pure HTML5, CSS3, and ES6+ JavaScript
- **Static data**: All data embedded in JavaScript objects
- **Browser compatibility**: Modern browsers (Chrome, Firefox, Edge, Safari)
- **Responsive tables**: Clean table styling with hover effects
- **URL parameters**: Details page uses `?trainId=UUID` query string

## Color Scheme

### Status Colors
- **Green** (#48bb78) - Open state, created action
- **Blue** (#4299e1) - Merged state, updated action
- **Gray** (#a0aec0) - Deleted state
- **Purple** (#9f7aea) - Merged action
- **Red** (#f56565) - Deleted action
- **Orange** (#ed8936) - Shared action
- **Pink** (#ed64a6) - Unshared action

### Primary Colors
- **Primary** (#2c5282) - Headers, titles
- **Secondary** (#4a5568) - Text
- **Background** (#f7fafc) - Page background
- **Card** (#ffffff) - Card background

## Data Sources

The implementation is based on:

- **OpenAPI Schema** (r2p-spec.yaml, train.yaml)
  - TrainListElement, Branch, Change schemas
  - BranchState enum (open, merged, deleted)
  - Action enum (created, updated, merged, deleted, shared, unshared)
  - PartnerToken enum (SBB, BLS, SOB)

- **Database Schema** (V1__create.sql)
  - train_version table
  - branch table
  - train_partner table
  - changelog table

## Browser Testing

Tested successfully on:
- ✅ Google Chrome (latest)
- ✅ Mozilla Firefox (latest)
- ✅ Microsoft Edge (latest)
- ✅ Safari (latest)

## Future Enhancements

When connecting to real backend API:
1. Replace static data with API calls to R2P backend
2. Implement merge request functionality
3. Add filtering and sorting capabilities
4. Add pagination for large datasets
5. Implement real-time updates
6. Add authentication and authorization

## Notes

- This is a **static prototype** for design demonstration only
- No backend connection or database access
- All data is hardcoded in JavaScript
- Designed for desktop browsers (not mobile-optimized)
- English language UI only

## Validation Checklist

✅ index.html displays all required sections with sample data  
✅ details.html shows all four sections correctly  
✅ Swiss date/time format applied consistently  
✅ Partner names shown in full (not abbreviations)  
✅ Table rows on index.html are clickable  
✅ Navigation from index to details works  
✅ All branch states visible in sample data (open, merged, deleted)  
✅ All changelog actions demonstrated (created, updated, merged, deleted, shared, unshared)  
✅ Clean, modern, professional design  
✅ No external dependencies used  
✅ Code is well-structured and commented  

---

**Created**: February 13, 2026  
**Version**: 0.0.12-SNAPSHOT  
**Lead RU**: Schweizerische Bundesbahnen (SBB)

