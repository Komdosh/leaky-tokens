# Performance Baseline Reports (2026-02-04)

Baseline Gatling runs executed with `docker compose -f docker-compose.full.yml up -d`
and `./gradlew :performance-tests:runGatlingAll`.

## Reports
- `analyticsquerysimulation-20260204105008555/`
- `authloginsimulation-20260204105051984/`
- `tokenconsumesimulation-20260204105135247/`
- `tokenpurchasesagasimulation-20260204105218639/`
- `tokenquotachecksimulation-20260204105301950/`
- `tokenusagepublishsimulation-20260204105345268/`

## Notes
- The analytics query simulation logged connection refused errors against `http://localhost:8083`
  early in the run (analytics service startup race). The report is still generated but will
  include those failures.
