# Plan: UI Design v2 Improvements

Überarbeiteter Designvorschlag für das R2P-UI basierend auf der ersten Iteration. Die v2-Version implementiert kompaktere Layouts, erweiterte Filter- und Sortierfunktionen, Paginierung für grosse Datensätze, verbesserte Barrierefreiheit und optimierte Darstellung von Partner-Informationen.

## Steps

### 1. Erstellen von design/v2/index.html
**Ziel:** Kompakte Übersichtsseite mit erweiterter Funktionalität

**Änderungen am Header:**
- Version als Badge ganz oben rechts positionieren
- Lead RU direkt unter dem Haupttitel als Unterzeile
- Kompakterer Header ohne separate System-Info-Sektion

**Änderungen an der Statistik:**
- Kleinere Stat-Cards mit kompakterem Layout
- Dropdown-Filter zur Einschränkung auf spezifische Partner
- Zusätzlicher Zähler "Open Branches" neben "Open Merge Requests"
- Statistik aktualisiert sich basierend auf Partner-Auswahl

**Änderungen an der Trains-Tabelle:**
- Filter-Bar zwischen Überschrift und Tabelle:
  - Betriebstag-Filter (Start/End Date) - Default: nächste 7 Tage
  - Text-Filter für Train Number
  - Dropdown-Filter für Partner
  - Checkbox-Filter für "Has Open Merge Requests" und "Has Open Branches"
- Sortierung für alle Spalten (Train Number, Operational Day, Last Change)
  - Visuelle Indikatoren (▲/▼) für aktive Sortierung
- Paging mit 20 Einträgen pro Seite
  - Navigation: « Previous | Page X of Y | Next »
  - Anzeige: "Showing 1-20 of 87 trains"
- Spalten-Umstrukturierung:
  - "Lead RU" Spalte mit Abkürzung (z.B. "SBB")
  - "Partners" Spalte mit Badges für Partner (ohne Lead RU)
  - Partner-Badges mit farblicher Hinterlegung
- Visuelle Indikatoren in der Tabelle:
  - Icon für offene Merge Requests (z.B. 🔀 mit Zahl)
  - Icon für offene Branches (z.B. 🌿 mit Zahl)
- Klickbare Zeilen mit deutlichem Hover-Effekt
- Icon am Ende jeder Zeile (→ oder ⟩) für bessere UX

### 2. Erstellen von design/v2/details.html
**Ziel:** Detailseite mit kompaktem Layout und erweiterten Tabellenfunktionen

**Änderungen am General Information Bereich:**
- Kompakteres Grid-Layout (horizontal statt 2x2)
- Kleinere Schrift und weniger Padding
- Integration in Header-Bereich

**Änderungen an Merge Requests Sektion:**
- Kompakte Empty-State mit kleinerem Icon und Text
- Reduziertes Padding wenn leer
- Weiterhin sichtbar aber platzsparend

**Änderungen an Branches-Tabelle:**
- Filter-Bar:
  - Dropdown-Filter für Status (All, Open, Merged, Deleted)
  - Dropdown-Filter für Owner (All, SBB, BLS, SOB, etc.)
- Sortierung für Name, Owner, Creation Date
  - Visuelle Indikatoren (▲/▼) für aktive Sortierung
- Paging mit 20 Einträgen pro Seite
- Branch ID Spalte:
  - Nur erste 8 Zeichen anzeigen (z.B. "b50e8400...")
  - Clipboard-Button (📋) zum Kopieren der vollständigen ID
  - Tooltip bei Hover mit vollständiger ID
- Owner mit Abkürzungen (SBB, BLS, SOB)
- State Badges mit verbesserter Barrierefreiheit:
  - Höherer Kontrast
  - Zusätzliche Symbole: ✓ Open, ✓✓ Merged, ✗ Deleted
  - ARIA-Labels für Screen Reader

**Änderungen an Changelog-Tabelle:**
- Filter-Bar:
  - Dropdown-Filter für Action (All, Created, Updated, Merged, etc.)
  - Dropdown-Filter für Partner (All, SBB, BLS, SOB, etc.)
  - Text-Filter für Comment
- Paging mit 20 Einträgen pro Seite
- Version ID Spalte:
  - Nur erste 8 Zeichen anzeigen
  - Clipboard-Button (📋) zum Kopieren der vollständigen ID
  - Tooltip bei Hover mit vollständiger ID
- Partner mit Abkürzungen
- Action Badges mit verbesserter Barrierefreiheit:
  - Höherer Kontrast
  - Zusätzliche Symbole: ➕ Created, ✏️ Updated, 🔀 Merged, ❌ Deleted, 🔗 Shared, ⛓️‍💥 Unshared
  - ARIA-Labels für Screen Reader

### 3. Erstellen von design/v2/style.css
**Ziel:** Erweiterte Styles für neue Funktionalität und verbesserte Barrierefreiheit

**Neue CSS-Komponenten:**
- `.version-badge` - Kompaktes Version-Badge oben rechts
- `.lead-ru-subtitle` - Styling für Lead RU unter Titel
- `.compact-header` - Kompakter Header-Bereich
- `.compact-stat-card` - Kleinere Statistik-Karten
- `.filter-bar` - Container für Filter-Controls
- `.filter-group` - Einzelne Filter-Gruppe (Label + Input)
- `.filter-input`, `.filter-select` - Styling für Filter-Inputs
- `.sort-header` - Klickbare Tabellen-Header mit Sortierung
- `.sort-indicator` - Sortierungs-Symbole (▲/▼)
- `.pagination` - Paging-Navigation
- `.pagination-info` - "Showing X-Y of Z" Text
- `.partner-badge` - Farbige Partner-Badges
- `.indicator-icon` - Icons für Merge Requests/Branches
- `.clipboard-btn` - Clipboard-Button-Styling
- `.action-icon`, `.state-icon` - Icons für Actions/States

**Verbesserte Barrierefreiheit:**
- WCAG 2.1 Level AA Kontrast-Verhältnisse (mindestens 4.5:1 für Text)
- Zusätzliche visuelle Indikatoren neben Farben
- Focus-Styles für Tastaturnavigation
- ARIA-Support im CSS (für aria-sort, aria-label, etc.)

**Responsive Anpassungen:**
- Filter kollabieren auf mobilen Geräten
- Tabellen horizontal scrollbar auf kleinen Bildschirmen
- Kompaktere Paging-Navigation auf Mobile

### 4. JavaScript-Funktionalität erweitern
**Ziel:** Interaktive Filter-, Sortier- und Paging-Funktionen

**Für index.html:**
- `filterTrains()` - Filtert Züge basierend auf:
  - Betriebstag-Range (Start/End Date)
  - Train Number (Text-Match)
  - Partner (Dropdown)
  - Has Open Merge Requests (Checkbox)
  - Has Open Branches (Checkbox)
- `sortTrains(column, direction)` - Sortiert Tabelle
  - Unterstützte Spalten: trainNumber, operationalDay, lastChange
  - Toggle zwischen ASC/DESC
- `paginateTrains(page)` - Zeigt spezifische Seite
  - Navigation: Previous, Next, Direct Page
  - Update der Pagination-Info
- `filterStatistics(partner)` - Aktualisiert Statistik-Karten basierend auf Partner-Filter
- `copyToClipboard(text)` - Kopiert Text in Zwischenablage mit Feedback

**Für details.html:**
- `filterBranches(status, owner)` - Filtert Branches
- `sortBranches(column, direction)` - Sortiert Branch-Tabelle
- `paginateBranches(page)` - Branch-Paging
- `filterChangelog(action, partner, comment)` - Filtert Changelog
- `paginateChangelog(page)` - Changelog-Paging
- `copyToClipboard(text)` - Clipboard-Funktion
- `truncateId(id, length)` - Kürzt IDs für Anzeige

**Gemeinsame Hilfsfunktionen:**
- `formatSwissDate(dateString)` - Bereits vorhanden, ggf. erweitern
- `formatSwissTimestamp(dateString)` - Bereits vorhanden
- `expandPartnerNames(partners)` - Anpassen für Abkürzungen
- `getPartnerBadgeClass(partner)` - Neue Funktion für Partner-Badges
- Event-Listener für alle Filter-, Sortier- und Paging-Elemente

### 5. Sample-Data erweitern
**Ziel:** Realistische Testdaten für alle neuen Funktionen

**Erweiterungen:**
- Mindestens 50 Züge für realistisches Paging (aktuell nur 4)
- Diverse Partner-Kombinationen:
  - Single-Partner Züge (nur SBB)
  - Multi-Partner Züge (SBB + BLS + SOB + weitere)
  - Verschiedene Lead RUs (nicht nur SBB)
- Zeitliche Verteilung:
  - Züge in verschiedenen Betriebstag-Ranges
  - Einige in den nächsten 7 Tagen (für Default-Filter)
  - Einige außerhalb für Filter-Tests
- Branches und Merge Requests:
  - Einige Züge mit offenen Merge Requests
  - Einige Züge mit vielen offenen Branches
  - Einige Züge ohne offene Items
- Branches pro Zug:
  - Mindestens ein Zug mit >20 Branches für Paging-Test
  - Verschiedene Stati-Verteilungen
  - Verschiedene Owner
- Changelog pro Zug:
  - Mindestens ein Zug mit >20 Changelog-Einträgen
  - Alle Action-Typen vertreten
  - Verschiedene Partner
  - Diverse Comments für Filter-Tests

## Technical Considerations

### Icon-Strategie
**Option 1: Unicode-Symbole (EMPFOHLEN)**
- Vorteile: Keine Dependencies, sofort verfügbar, performant
- Nachteile: Limitierte Auswahl, plattformabhängiges Rendering
- Verwendung: ▲ ▼ → ⟩ 📋 🔀 🌿 ✓ ✗ ➕ ✏️ ❌ 🔗 ⛓️‍💥

**Option 2: Externe Icon-Library**
- Optionen: Font Awesome, Material Icons, Feather Icons
- Vorteile: Konsistentes Design, große Auswahl
- Nachteile: Zusätzliche Dependency, Ladezeit

**Entscheidung:** Unicode für Prototyp, kann später auf Library migriert werden

### Barrierefreiheit (WCAG 2.1 Level AA)
**Kontrast-Anforderungen:**
- Normal Text: mindestens 4.5:1
- Großer Text (18pt+): mindestens 3:0:1
- UI-Komponenten: mindestens 3:1

**Zusätzliche Maßnahmen:**
- ARIA-Labels für alle interaktiven Elemente
- ARIA-Live-Regions für dynamische Inhalte (Filter-Ergebnisse)
- Tastaturnavigation für alle Funktionen
- Focus-Indikatoren für alle fokussierbaren Elemente
- Icons immer mit Text oder aria-label

### Responsive Design
**Breakpoints:**
- Desktop: >1200px (Standard-Layout)
- Tablet: 768px-1199px (kompakte Filter)
- Mobile: <768px (gestapelte Filter, horizontales Scrollen für Tabellen)

**Mobile-Optimierungen:**
- Filter in collapsible Accordion
- Pagination mit weniger Buttons
- Tabellen mit horizontalem Scroll
- Kleinere Badges und Icons

### Performance-Überlegungen
- Client-seitige Filter/Sort/Paging für Prototyp
- Bei >1000 Einträgen sollte Server-seitiges Paging implementiert werden
- Debouncing für Text-Filter (300ms Verzögerung)
- Virtual Scrolling als Alternative zu Paging (für zukünftige Iteration)

### Datums-Handling
- Default Betriebstag-Filter: heute + 7 Tage
- Verwendung von ISO-8601 Formaten im JavaScript
- Anzeige in Swiss Format (DD.MM.YYYY)
- Browser-native Date-Inputs mit Fallback

## Implementation Order

1. **Start mit style.css** - Basis-Styles für alle neuen Komponenten
2. **Erweitern der Sample-Data** - Realistische Testdaten generieren
3. **index.html Header/Stats** - Kompakter oberer Bereich
4. **index.html Filter/Sort/Paging** - Tabellen-Funktionalität
5. **details.html General Info** - Kompakter Bereich
6. **details.html Branches** - Tabellen-Funktionalität
7. **details.html Changelog** - Tabellen-Funktionalität
8. **JavaScript-Funktionen** - Interaktivität implementieren
9. **Testing & Refinement** - Alle Features testen

## Success Criteria

- [ ] Header ist deutlich kompakter (max. 150px Höhe statt ~250px)
- [ ] Statistik-Karten sind ~30% kleiner
- [ ] Partner-Filter in Statistik funktioniert
- [ ] Alle 6 Filter-Funktionen in Trains-Tabelle funktionieren
- [ ] Sortierung mit visuellen Indikatoren funktioniert
- [ ] Paging zeigt korrekt 20 Einträge pro Seite
- [ ] Partner werden als Badges mit Abkürzungen dargestellt
- [ ] Klickbare Zeilen sind deutlich erkennbar
- [ ] Branch/Changelog-Tabellen haben funktionales Paging
- [ ] IDs sind gekürzt mit funktionalem Clipboard-Button
- [ ] Alle Badges haben ausreichenden Kontrast (>4.5:1)
- [ ] Icons ergänzen Farben für bessere Zugänglichkeit
- [ ] Responsive Design funktioniert auf Mobile/Tablet
- [ ] Keine JavaScript-Errors in Console
- [ ] Alle interaktiven Elemente sind per Tastatur bedienbar

