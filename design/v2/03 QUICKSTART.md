# R2P UI v2 - Quick Start Guide

## Getting Started in 30 Seconds

1. **Open the prototype:**
   - Navigate to `C:\devsbb\git\Github\r2p\design\v2\`
   - Open `index.html` in your web browser

2. **You'll see:**
   - Compact header with version badge (top right)
   - 5 statistics cards
   - A table with 50 sample trains

3. **Try these features:**
   - Click any train row to see details
   - Use filters above the table
   - Click column headers to sort
   - Navigate between pages with pagination controls

## Main Features Overview

### 🏠 Index Page (Overview)

**Filter Trains:**
- Type train number (e.g., "IC") in search box
- Select date range (default: next 7 days)
- Choose specific partner from dropdown
- Check boxes for trains with open merge requests or branches

**Sort Trains:**
- Click "Train Number", "Operational Day", or "Last Change" headers
- Click again to reverse sort direction
- See ▲/▼ indicator for current sort

**Navigate:**
- Use pagination controls at bottom
- Shows "Showing 1-20 of 50 trains"
- Buttons: « First | ‹ Previous | Page X of Y | Next › | Last »

**View Statistics by Partner:**
- Select partner in dropdown above statistics
- Statistics update to show only that partner's data

### 📋 Details Page

**General Information:**
- Compact layout shows train details
- Click 📋 button next to Train ID to copy full ID

**Branches:**
- Filter by Status (Open/Merged/Deleted)
- Filter by Owner (SBB/BLS/SOB/etc.)
- Sort by Name, Owner, or Creation Date
- Click 📋 to copy full Branch ID
- Navigate with pagination (20 per page)

**Changelog:**
- Filter by Action type
- Filter by Partner
- Search in comments
- Click 📋 to copy full Version ID
- Navigate with pagination (20 per page)

## Key Improvements Over v1

✅ **Compact Design** - Header 40% smaller, better use of space
✅ **Advanced Filtering** - Multiple filter criteria on all tables
✅ **Pagination** - Handle large datasets efficiently (20 items/page)
✅ **Sorting** - Click headers to sort any column
✅ **Better Accessibility** - Keyboard navigation, screen reader support
✅ **Visual Indicators** - Icons (🔀 🌿) show merge requests and branches
✅ **Color-Coded Badges** - Partners have distinct colors (SBB=red, BLS=blue, SOB=green)
✅ **Copy IDs** - One-click copy for all UUIDs
✅ **More Test Data** - 50 trains instead of 4

## Common Tasks

### Task: Find trains for a specific partner
1. In statistics section, select partner from dropdown
2. OR use "Partner" filter above train table
3. Results update automatically

### Task: View trains in next week
1. Date filter is pre-set to next 7 days
2. Adjust "Op. Day From" and "Op. Day To" as needed
3. Table updates automatically

### Task: Copy a Branch ID
1. Click train to view details
2. Scroll to Branches section
3. Click 📋 button next to truncated ID
4. See "Copied to clipboard!" notification

### Task: Find all merge actions
1. Open train details
2. Scroll to Changelog
3. Select "Merged" in Action filter
4. Only merge actions are shown

### Task: Sort trains by last change
1. On index page
2. Click "Last Change" column header
3. Click again to reverse order
4. See ▲ or ▼ indicator

## Keyboard Navigation

- **Tab** - Move between controls
- **Shift+Tab** - Move backwards
- **Enter/Space** - Activate buttons, checkboxes, sort headers
- **Arrow Keys** - Navigate within dropdowns

All interactive elements have visible focus indicators (blue outline).

## Browser Requirements

- Modern browser (Chrome 90+, Firefox 88+, Safari 14+, Edge 90+)
- JavaScript enabled
- No plugins required
- Works offline (all resources local)

## Test Data Information

The prototype includes:
- **50 trains** with various characteristics
- **Partners:** SBB, BLS, SOB, DB, ÖBB, SNCF, Trenitalia
- **Operational Days:** Distributed from 5 days ago to 14 days ahead
- **Train #5** has 25+ branches (for pagination testing)
- Various states: open, merged, deleted branches
- All action types in changelog: created, updated, merged, deleted, shared, unshared

## Tips & Tricks

💡 **Filter Combinations** - Use multiple filters together for precise results
💡 **Date Reset** - Clear date filters to see all trains
💡 **Partner Statistics** - Use statistics filter to see partner-specific metrics
💡 **Text Search** - Filters have 300ms debounce (stops filtering until you finish typing)
💡 **Responsive** - Try resizing browser window to see mobile layout
💡 **Clickable Rows** - Hover over train rows to see → indicator

## Troubleshooting

**Problem:** No trains shown
- Check date filters - they default to next 7 days only
- Clear other filters (Train Number, Partner, checkboxes)

**Problem:** Clipboard copy doesn't work
- Browser may block clipboard access
- Check browser permissions
- Use HTTPS or localhost for better compatibility

**Problem:** Table looks cramped on mobile
- Tables scroll horizontally on small screens
- Rotate device to landscape for better view
- Filter options collapse into vertical layout

**Problem:** Pagination shows "Page 1 of 1" even with many items
- Filters may be hiding results
- Check active filters and clear as needed

## Known Limitations

⚠️ **Client-side only** - All filtering/sorting happens in browser (suitable for <1000 items)
⚠️ **No persistence** - Filters reset on page reload
⚠️ **Static data** - Uses generated sample data, not live API
⚠️ **No merge requests** - Feature not yet implemented (shows empty state)

## Next Steps

After exploring the prototype:
1. Review `README.md` for detailed feature documentation
2. Check `plan-htmlUiDesignV2.prompt.md` for design decisions
3. Open browser DevTools to inspect implementation
4. Test accessibility with keyboard navigation
5. Try on mobile device or narrow browser window

## Feedback & Questions

This is a prototype for demonstration and design validation. For questions about implementation or feature requests, refer to the main R2P project documentation.

---

**Current Date:** February 13, 2026  
**Prototype Version:** 2.0.0  
**Last Updated:** February 13, 2026

