# Ausgangslage

Als Benutzer möchte ich das der Merge eine aktiv ausgeführte Operation und nicht etwas Implizites (bspw. via Status) ist. Zudem in einem atomic request abgewickelt (nicht mehrere Requests notwendig für die effektive Merge Aktion). Der Mechanismus soll zudem einfach gestaltet sein (bspw. keine Angabe einer Basisversion erforderlich, keine Multi-Diff zwischen verschiedenen Ständen)

# AK

* Mit einem neuen Endpunkt kann der Ersteller eines Branches einen Merge Request erstellen. Dabei kann ein Kommentar angegeben werden. Der Merge Request wird in der Änderungshistory festgehalten. Dadurch ist er für das Lead RU (und alle anderen Partner) sichtbar.
* Mit einem neuen Endpunkt kann das Lead RU einen Merge von einem Branch auf Main vornehmen. Er gibt dabei die von ihm geprüfte Version an (falls der Branch unterdessen verändert wurde gibt es eine Exception). Dabei kann optional auf einen MergeRequest verwiesen werden (aber auch ohne offenen Merge Request ist ein Merge möglich). Es kann ein Kommentar ergänzt werden. Der Branch wird geschlossen und sein Stand auf Main übertragen. In der Änderunghistory ist neben dem Kommentar sowohl die Schliessung des Branches als auch die Übernahme auf Main ersichtlich (inklusive Angabe der VersionsId die übernommen wurde).
* Mit einem neuen Endpunkt kann das Lead RU einen Merge Request ablehnen. Dies wird mit einem Kommentar in der History vermerkt. Der ursprüngliche Branch bleibt geöffnet und der Main Branch bleibt unverändert.
* Die drei neuen Endpunkte sind per Unit Test überprüft
  * Zusätzlicher Test für ein AK aus einer bereits implementierten Story (BPSGLO-13863):  Wird eine Operation auf einem gelöschten oder gemergten Branch ausgeführt, kommt ein Fehler (keine Änderungen mehr möglich)
* Open Merge Requests in der Statistik auf der Benutzeroberfläche ergänzen.

# Vorschlag für die API

* POST /v1/trains/{trainId}/{branchId}/requestMerge
* POST /v1/trains/{trainId}/{branchId}/merge
* POST /v1/trains/{trainId}/{branchId}/rejectMerge

# Abgrenzung

* Es ist nur möglich den aktuellen Stand des Branches zu übernehmen. D. h. es ist nicht möglich gleichzeitig noch eine weitere Änderung zu machen oder ähnliches
* Es gibt kein Konflikthandling. Es liegt in der Verantwortung des Lead RU den Stand auf Main zu vergleichen mit dem Stand des Branches vor einem Merge. Zur Sicherheit wird die "gesichtete" Version angegeben (Erkennung von Änderungen am Branch) und bei mismatch verworfen.
* Kein Umbau des Changelog (bspw. stärkere Aufteilung)