# User Story Designvorschlag

## AK
* Erstelle HTML-Seiten für einen Designvorschlag der Benutzeroberfläche im Ordner design.
* Es gibt zwei Seiten: index.html und details.html.
* Es wird ein schlichtes aber modernes Design verwendet.
* Es sind keine externen Bibliotheken oder Frameworks erlaubt, nur reines HTML, CSS und Javascript
* index.html zeigt eine Übersicht des im System befindlichen Verkehrs
   * Einführung mit einer kurzen Erklärung von R2P.
   * Anzeige des Lead RU (instance owner) und der Serverversion
   * Anzeige einer kleinen Statistik: Anzahl Verkehre, Anzahl Branches, Anzahl Versionen, Anzahl offener Merge Requests
   * Tabellarische Auflistung der Verkehre mit Spalten: Zugnummer, Betriebstag, Partner, Letzte Änderung
   * Jede Zeile der Tabelle ist klickbar und führt zu details.html mit den Details des ausgewählten Verkehrs. Im Rahmen des Poc kann jede Zeile mit einem statischen Link zu details.html versehen werden, da die Details ja auch statisch sind.
* details.html zeigt die Details eines ausgewählten Verkehrs
   * Die Details bestehend aus vier Abschnitten: Allgemeine Informationen, Merge Requests, Branches und Changelog
   * In den Allgemeinen Informationen ist die Zugid, Zugnummer, Betriebstag und Partner aufgeführt.
   * In Merge Requests werden die offenen Merge Requests mit ID, Ersteller, Erstellungsdatum und Kommentar nach Erstellungsdatum absteigend sortiert angezeigt.
   * In Branches werden die Branches mit ID, Name, Ersteller, Erstellungsdatum und Status (open, merged, deleted) nach Erstellungsdatum absteigend angezeigt.
   * In Changelog werden die letzten Änderungen mit Action (created, updated, merged, deleted, shared, unshared), Timestamp, Branchname, VersionId, Partner und Kommentar nach Datum absteigend sortiert angezeigt.

## Abgrenzung
* Es werden keine Aktionen (z.B. Erstellen, Bearbeiten, Löschen von Verkehren) implementiert, sondern nur die Anzeige der Informationen.
* Die Seiten mussen nicht responsiv sein. D. h. es reicht wenn diese auf einem normalen Desktop-Browser gut aussehen und funktionieren.
* Der Designentwurf kann statisch sein, d. h. es muss keine Anbindung an eine API oder Datenbank geben. Es können statische Daten verwendet werden, um die Anzeige zu demonstrieren.

## Weitere Informationen
* Merge Requests sind aktuell noch nicht implementiert aber kommen noch.
* Partner entspricht einer Liste von EVU: dem Lead RU und den weiteren Partnern mit denen der Verkehr geteilt wurde
* Nutze die OpenAPI Spezifikation (r2p-spec.yaml und train.yaml) um die benötigten Informationen für die Anzeige zu erhalten.
* Nutze das Datenbankschema V1__create.sql um die benötigten Informationen für die Anzeige zu erhalten.
* Zeit- und Datum sollen schweizerisch formatiert angezeigt werden (z.B. 01.01.2024 12:00:00)
* Die Sprache der Benutzeroberfläche soll Englisch sein.
* Das Lead RU und die Partner sollen jeweils mit ihrem Namen (nicht nur Abkürzung) aufgeführt werden.