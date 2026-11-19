# team-arbeidsforhold-statistikk

Team arbeidsforhold's github-app og oversikt-side. Appen er avgrenset til teamet, i produksjon.

## Lokal kjøring

Du trenger Java 25, Maven, Docker med Compose og verktøyversjonene i `mise.toml`.
Kjør fra prosjektroten:

```bash
docker compose -f compose.local.yaml up -d --wait
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Åpne http://127.0.0.1:8080. Maven genererer API-klientene og bygger frontend-en,
som Spring serverer sammen med API-et. Etter frontend-endringer må du stoppe appen
og kjøre kommandoen på nytt.

Når appen har startet og Liquibase har opprettet tabellene, last inn syntetiske
eksempeldata fra en annen terminal:

```bash
docker compose -f compose.local.yaml exec -T database sh -c \
  'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < local/eksempeldata.sql
```

Oppdater nettleseren for å se repoer, ingresser og sårbarheter.
Du kan kjøre SQL-filen flere ganger uten å få duplikater eller overskrive egne data.
Ingressadressene og CVE-ID-ene i eksempeldataene er fiktive.

Databasen ligger på `127.0.0.1:5432`, heter `statistikk` og har bruker og passord
`local`. Du kan legge inn flere eksempler med en SQL-klient. Databasen beholder
innholdet ved omstart og ved `docker compose -f compose.local.yaml down`.

Standardverdiene ligger i `application-local.yaml`. Sett miljøvariablene
`LOCAL_DB_PORT`, `LOCAL_DB_USERNAME` og `LOCAL_DB_PASSWORD` før begge
oppstartskommandoene for å overstyre dem. `LOCAL_DB_URL` lar appen bruke en
eksisterende lokal PostgreSQL, for eksempel
`jdbc:postgresql://127.0.0.1:5433/statistikk`. Både appen og Liquibase bruker denne
adressen. URL-en må bruke en loopback-adresse og kan ikke inneholde
innloggingsverdier eller URL-parametere. Hvis du endrer bruker eller passord,
beholder en eksisterende database i Docker-volumet de gamle verdiene.

Lokalprofilen tillater anonym tilgang, avviser admin-handlinger og deaktiverer
GitHub-oppdateringer og eksterne importer. Bruk bare syntetiske data og en egen
lokal database, ikke en tunnel til en delt database. Appen avviser lokalprofilen
sammen med andre profiler, kjente kjøremiljøvariabler eller eksterne
databaseadresser. Uten `local` gjelder eksisterende autentisering og
integrasjoner uendret.

## Henvendelser

Spørsmål knyttet til koden eller repositoryet kan stilles som issues her på GitHub

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-arbeidsforhold
