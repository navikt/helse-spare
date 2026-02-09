# Spare
![Bygg og deploy](https://github.com/navikt/helse-spare/workflows/bygg%20og%20deploy/badge.svg)

Sparer meldinger på rapiden og lagrer dem i databasen.

Kolonnen json i tabellen melding har snål formatering. Dette må til for å få gyldig JSON:
`replace(replace(replace(json::text, '\"', '"'), '"{', '{'), '}"', '}')::json`.

For spørringer via federated query (BigQuery) gjøres det slik:
`replace(replace(replace(replace(json::text, chr(92) || chr(92) || chr(92) || '"', ''), chr(92) || '"', '"'), '"{', '{'), '}"', '}')::json`

## Oppgradering av gradle wrapper
Finn nyeste versjon av gradle her: https://gradle.org/releases/

```./gradlew wrapper --gradle-version $gradleVersjon```

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
