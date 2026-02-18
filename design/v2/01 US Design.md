# User Story Designvorschlag Iteration 2

## Kontext
* Es wurde im Ordner v1 bereits eine erste Iteration des Designvorschlags generiert (siehe index.html, details.html, style.css). Diese soll nun überarbeitet und verbessert werden.

## AK
* Die bestehenden Dateien sollen nicht mehr verändert werden. Im Ordner design/v2 sollen neue Dateien erstellt werden, die die überarbeitete Version des Designvorschlags enthalten.
* Verbesserung des oberen Bereichs der index.html Seite (Titel, Lead-RU, Serverversion und Statistik)
  * Es wird zuviel Platz eingeommmen. Bitte deutlich kompakter gestalten
  * Die Version könnte ganz oben rechts auf der Seite stehen
  * Die Lead-RU könnte direkt unter dem Titel stehen
  * Die Statistik kompakter darstellen. Die Kärtchen und deren Inhalt sind aktuell eher zu gross.
  * Aktuell bezieht sich die Statistik auf den gesamten Verkehr. Es wäre sinnvoll, wenn diese bspw. per Dropdownauswahl auf einen Partner eingeschränkt werden kann.
  * In der Statistik neben "Open Merge Requests" könnte zusätzlich die Anzahl der offenen Branches angezeigt werden, da dies auch eine wichtige Information ist.
* Verbesserung der Trains Tabelle auf der index.html Seite 
  * Es soll filtern nach Train Number, Operational Day und Partner ermöglicht werden
  * Es soll sortieren nach Train Number, Operational Day und Last Change ermöglicht werden
  * Es ist unrealistisch alle Züge auf einmal anzuzeigen. Die Tabelle soll ein Paging erhalten (20 Einträge pro Seite) und die Gesamtanzahl der Seiten anzeigen
  * Zwischen der Überschrift Trains und der Tabelle soll es einen Filter geben um den Start- und Endbetriebstag einzuschränken. Standartmässig ist dieser auf die nächsten 7 Tage eingestellt.
  * Die aktuelle Partner Spalte soll aufgeteilt werden in Lead RU und Partner. Es soll jeweils die Abkürzung und nicht der volle Name verwendet werden. Die Partner sollen besser abgegrenzt/sichtbar werden bspw. durch farbliche Hinterlegung oder ein Badge. Das Lead RU muss in der Spalte Partner nicht aufgefhört werden, da es ja bereits in der Spalte Lead RU enthalten ist
  * Es soll klarer sein, dass man per Klick auf die Tabellenzeile zu den Details gelangt. Aktuell ist es nicht so offensichtlich, dass die Zeilen klickbar sind. Es könnte ein kleines Symbol am Ende der Zeile, das anzeigt, dass man reingehen kann ergänzt werden
  * Es wäre schön auf Züge filtern zu können, welche offene Merge Requests oder offene Branches haben. Zudem wäre es toll wenn in der Tabelle angezeigt werden könnte, ob es offene Merge Requests oder offene Branches gibt (z.B. durch ein Symbol oder eine farbliche Markierung)
  * Darauf achten, dass das Layout trotz Filter und Sortierung kompakt bleibt.  
* Verbesserung der details.html Seite oberer Bereich
  * Der obere Bereich mit den allgemeinen Informationen könnte kompakter gestaltet werden, damit mehr Platz für die anderen Abschnitte bleibt
* Verbesserung der details.html Seite Merge Requests
  * Die Sektion Merge Requests sollte im Fall, dass es keine Merge Requests gibt, kompakter dargestellt werden, damit nicht so viel Platz mit der Nachricht "No merge requests available" eingenommen wird. Sie soll aber weiterhin dargestellt werden
* Verbesserung der details.html Seite Branches
  * Es ist unrealistisch alle Branches auf einmal anzuzeigen. Die Tabelle soll ein Paging erhalten (20 Einträge pro Seite) und die Gesamtanzahl der Seiten anzeigen
  * Es soll ein Filter geben um die Branches nach Status (open, merged, deleted) und Owner zu filtern
  * Es soll eine Sortierung nach Name, Owner und Creation Date ermöglicht werden
  * Die Spalte Branch ID ist etwas breit. Es wäre ok die Branch Id nicht vollständig anzuzeigen, dafür ein Symbol "Clipboard" zu ergänzen um die vollständige Branch ID in die Zwischenablage zu kopieren
  * Der Owner soll jeweils mit Abkürzung und nicht mehr mit vollem Namen angezeigt werden. 
  * Die Farben der States sind nicht optimal (falls jemand farbenblind ist), könnnte das noch etwas optimiert werden, bspw. besserer Kontrast oder zusätzliches Symbol
* Verbesserung der details.html Seite Changelog 
  * Es ist unrealistisch alle Changelog Einträge auf einmal anzuzeigen. Die Tabelle soll ein Paging erhalten (20 Einträge pro Seite) und die Gesamtanzahl der Seiten anzeigen
  * Es soll ein Filter geben um die Changelog Einträge nach Action, Partner und Comment zu filtern
  * In der Spalte Versions Id ein Clipboardsymbol ergänzen um die vollständige Version Id in die Zwischenablage zu kopieren
  * Der Partner soll jeweils mit Abkürzung und nicht mehr mit vollem Namen angezeigt werden.
  * Die Farben der Actions sind nicht optimal (falls jemand farbenblind ist), könnnte das noch etwas optimiert werden, bspw. besserer Kontrast oder zusätzliches Symbol

## Allgemeine Vorgaben
* Das Layout soll kompakt und übersichtlich bleiben.