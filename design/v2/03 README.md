# R2P UI Design v2

Enhanced HTML prototype for the R2P (Railway Undertaking Partner Communication) system.

## Overview

This is version 2 of the R2P UI prototype, featuring significant improvements over v1:
- **Compact layouts** with 30% space reduction
- **Advanced filtering** with multiple criteria
- **Pagination** for large datasets (20 items per page)
- **Improved accessibility** (WCAG 2.1 Level AA compliant)
- **Better visual indicators** with icons and badges
- **Responsive design** for mobile and tablet devices

## Files

- `index.html` - Main overview page with train list
- `details.html` - Detailed train view with branches and changelog
- `style.css` - Complete stylesheet with all components
- `sampleData.js` - Extended sample data (50 trains)
- `README.md` - This file
- `QUICKSTART.md` - Quick start guide

## Features

### Index Page (index.html)

**Header:**
- Compact design with version badge
- Lead RU displayed as subtitle
- ~40% space reduction compared to v1

**Statistics:**
- 5 metric cards (Trains, Branches, Versions, Open Branches, Open Merge Requests)
- Partner filter to view statistics for specific partners
- Smaller cards with better information density

**Trains Table:**
- **Filtering:**
  - Train Number (text search with debouncing)
  - Operational Day range (default: next 7 days)
  - Partner selection
  - Has Open Merge Requests (checkbox)
  - Has Open Branches (checkbox)
- **Sorting:**
  - All columns sortable (Train Number, Operational Day, Last Change)
  - Visual indicators (▲/▼) for sort direction
  - Keyboard accessible
- **Pagination:**
  - 20 trains per page
  - Navigation: First, Previous, Next, Last
  - Shows current page and total pages
  - Shows item range (e.g., "Showing 1-20 of 50 trains")
- **Display:**
  - Separate Lead RU and Partners columns
  - Color-coded partner badges (SBB: red, BLS: blue, SOB: green)
  - Visual indicators for open merge requests (🔀) and branches (🌿)
  - Arrow indicator (⟩) on hover for clickable rows

### Details Page (details.html)

**General Information:**
- Horizontal layout for better space efficiency
- Truncated Train ID with clipboard button
- Lead RU and Partners with color-coded badges

**Branches Table:**
- **Filtering:**
  - Status (All, Open, Merged, Deleted)
  - Owner (dropdown with all available owners)
- **Sorting:**
  - Name, Owner, Creation Date
  - Visual indicators for sort direction
- **Pagination:**
  - 20 branches per page
  - Full navigation controls
- **Display:**
  - Truncated Branch IDs with clipboard buttons
  - Owner as badges
  - State badges with icons (✓ Open, ✓✓ Merged, ✗ Deleted)
  - Improved contrast for better accessibility

**Changelog Table:**
- **Filtering:**
  - Action type (All, Created, Updated, Merged, Deleted, Shared, Unshared)
  - Partner (dropdown)
  - Comment (text search with debouncing)
- **Pagination:**
  - 20 entries per page
  - Full navigation controls
- **Display:**
  - Truncated Version IDs with clipboard buttons
  - Partner as badges
  - Action badges with icons (➕ Created, ✏️ Updated, 🔀 Merged, ❌ Deleted, 🔗 Shared, ⛓️‍💥 Unshared)
  - Improved contrast for better accessibility

## Accessibility Features

- **WCAG 2.1 Level AA compliant:**
  - Minimum 4.5:1 contrast ratio for text
  - Minimum 3:1 contrast ratio for UI components
- **Keyboard navigation:**
  - All interactive elements are keyboard accessible
  - Tab order follows logical flow
  - Enter/Space to activate buttons and sortable headers
- **Screen reader support:**
  - ARIA labels on all interactive elements
  - ARIA-sort attributes on sortable columns
  - Semantic HTML structure
- **Visual indicators:**
  - Icons supplement color coding
  - Focus indicators on all focusable elements
  - Clear hover states

## Technical Details

### Browser Compatibility
- Modern browsers (Chrome, Firefox, Safari, Edge)
- ES6+ JavaScript features
- CSS Grid and Flexbox layouts
- Native date inputs with fallbacks

### Data Structure
The sample data includes:
- 50 trains with diverse characteristics
- Various partner combinations (SBB, BLS, SOB, DB, ÖBB, SNCF, Trenitalia)
- Multiple operational day ranges
- Realistic branch and changelog data
- One train (#5) with 25+ branches for pagination testing

### Performance
- Client-side filtering and sorting (suitable for <1000 items)
- Debounced text input (300ms delay)
- Pagination to limit DOM elements
- Minimal dependencies (no external libraries)

### Responsive Breakpoints
- **Desktop:** >1200px (full layout)
- **Tablet:** 768px-1199px (compact filters)
- **Mobile:** <768px (stacked filters, scrollable tables)

## Customization

### Colors
All colors are defined as CSS variables in `style.css`:
```css
:root {
    --primary-color: #2c5282;
    --accent-color: #3182ce;
    --partner-sbb: #eb0000;
    --partner-bls: #00539b;
    --partner-sob: #009b3a;
    /* ... */
}
```

### Partner Badges
To add a new partner color:
1. Add CSS variable in `:root`
2. Add CSS class `.partner-badge-ACRONYM`
3. Update `getPartnerBadgeClass()` function in JavaScript

### Pagination
To change items per page, modify:
```javascript
const itemsPerPage = 20; // Change this value
```

## Future Enhancements

Potential improvements for future versions:
- Server-side pagination for >1000 items
- Virtual scrolling as alternative to pagination
- Export functionality (CSV, PDF)
- Advanced filters (date ranges with presets)
- Saved filter preferences
- Real-time updates via WebSocket
- Merge request implementation
- Inline editing capabilities

## Testing

To test all features:
1. Open `index.html` in a browser
2. Test filters:
   - Enter "IC" in Train Number filter
   - Set date range to next 7 days
   - Check "Has Open Branches" checkbox
3. Test sorting:
   - Click column headers to sort
   - Verify visual indicators
4. Test pagination:
   - Navigate through pages
   - Verify counts are correct
5. Test accessibility:
   - Navigate with Tab key
   - Use Enter/Space to activate controls
   - Test with screen reader

## Comparison with v1

| Feature | v1 | v2 |
|---------|----|----|
| Header height | ~250px | ~150px |
| Statistics cards | 4 (large) | 5 (compact) |
| Train table filters | None | 6 filters |
| Sorting | None | 3 sortable columns |
| Pagination | None | 20 per page |
| Partner display | Text list | Color badges |
| Status indicators | None | Icons with counts |
| Branch filtering | None | 2 filters |
| Branch pagination | None | 20 per page |
| ID display | Full | Truncated + clipboard |
| Changelog filtering | None | 3 filters |
| Changelog pagination | None | 20 per page |
| Accessibility | Basic | WCAG 2.1 AA |
| Sample data | 4 trains | 50 trains |

## License

This prototype is part of the R2P project. See main project README for license information.

## Version History

- **v2.0.0** (2026-02-13) - Complete redesign with advanced features
- **v1.0.0** (2026-02-10) - Initial prototype

## Contact

For questions or feedback, please refer to the main R2P project documentation.

