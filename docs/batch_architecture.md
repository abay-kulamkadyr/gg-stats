# Batch Architecture (Updated)

This document describes batch-processing architecture of `gg-stats`, reverse-engineered from the repository state. It supersedes any previous batch docs.

References (paths):
- Scheduler: `src/main/java/com/abe/gg_stats/service/BatchSchedulerService.java`
- Job launcher config: `src/main/java/com/abe/gg_stats/config/batch/BatchConfiguration.java`
- Job definitions:
  - Heroes: `src/main/java/com/abe/gg_stats/config/batch/HeroesBatchConfig.java`
  - Teams: `src/main/java/com/abe/gg_stats/config/batch/TeamsBatchConfig.java`
  - Notable (Pro) Players: `src/main/java/com/abe/gg_stats/config/batch/NotablePlayersBatchConfig.java`
  - Players: `src/main/java/com/abe/gg_stats/config/batch/PlayersBatchConfig.java`
  - Hero Rankings: `src/main/java/com/abe/gg_stats/config/batch/HeroRankingsBatchConfig.java`
  - New Pro Matches ingestion: `src/main/java/com/abe/gg_stats/config/batch/NewMatchesJobConfig.java`
  - Historical Pro Matches ingestion: `src/main/java/com/abe/gg_stats/config/batch/HistoricalMatchesJobConfig.java`
  - Expiration config: `src/main/java/com/abe/gg_stats/config/batch/BatchExpirationProperties.java`
  - Application properties: `src/main/resources/application.properties`


## Overview
Batch subsystem collects data from OpenDota, processes it with Spring Batch, and stores it in PostgreSQL. It applies rate limiting and circuit breaker patterns to protect upstreams and uses scheduled jobs to keep data fresh. The frontend queries the processed data via REST controllers.

Key runtime actors visible in this repository:
- `StartupJobRunner` (optional, guarded by `app.startup.jobs.enabled`) to programmatically trigger jobs at startup.
- `SecurityConfig` allowing public access to application endpoints in the current state (suitable for local dev; restrict in prod).
- `BatchConfiguration` providing an async `JobLauncher`.

## High-Level Architecture
```
┌──────────────────────────────────────────────────────────────────────────┐
│                              GG Stats App                                │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Web Layer                     Service Layer                Batch Layer  │
│  ─────────                     ─────────────                ──────────   │
│  • Controllers (/heroes,       • OpenDota client(s)         • Job/Step   │
│    /highlights, /teams,        • Rate limiting               configs     │
│    /img, /api/monitoring/*)    • Circuit breaker            • Readers    │
│  • Aggregations trigger        • Aggregation service        • Processors │
│    (/api/aggregations/refresh) • Scheduler                  • Writers    │
│                                                                          │
│  Observability: Actuator (health, metrics), Http endpoints (/api/*)      │
└──────────────────────────────────────────────────────────────────────────┘
```

## Data Flow (batch jobs)
```
Scheduler ──cron──▶ Job/Step ──chunk-read──▶ Processor ──chunk-write──▶ DB
             ▲              (OpenDota API via readers)                     
             │
     Rate-limit & Circuit-breaker gate (skip/retry/backoff)
```

- Readers call OpenDota endpoints through client utilities/services.
- Fault tolerance per job config handles network and upstream conditions.
- Aggregations run after ingestion windows to materialize derived views.

## Configuration Hierarchy 
- `application.properties` (DB, actuator exposure, rate limiting, circuit breaker, batch tuning)
- `config/batch/*` for job/step wiring and policies
- `StartupJobRunner` guarded by `app.startup.jobs.enabled`

## Design Patterns
- Template Method (base batch listeners)
- Strategy (readers/processors/writers per domain)
- Observer (job/step/item listeners)
- Circuit Breaker and Rate Limiting (resilience)

## Runtime Overview
- __JobLauncher__: Custom async `TaskExecutorJobLauncher` configured in `BatchConfiguration` (profile `!test`). Jobs are launched asynchronously using a `SimpleAsyncTaskExecutor`.
- __Scheduling__: `BatchSchedulerService` uses `@Scheduled(cron = ...)` to trigger jobs periodically, with a pre-check against API limits via `OpenDotaRateLimitingService` (`canRunJob()` gate).
- __Rate-Limit Gate__: Before any scheduled job, remaining daily requests are fetched from `OpenDotaRateLimitingService`. If below a threshold (ex.50), the job is skipped with a warning log.
- __Manual Triggers__: `BatchSchedulerService` provides `trigger*` methods to manually invoke each job programmatically.
- __Aggregations__: Non-Batch aggregation step via `AggregationService#refreshPatchesAndAggregations()` runs on a schedule and can also be triggered via REST endpoint `/api/aggregations/refresh`.

## Scheduled Jobs & Crons
From `BatchSchedulerService`:
- __New Matches Ingestion__: `0 */15 * * * *` (every 15 minutes)
- __Historical Matches Ingestion__: `0 0 3 * * *` (daily at 03:00)
- __Heroes Update__: `0 0 2 * * *` (daily at 02:00)
- __Pro Players Update__: `0 0 */6 * * *` (every 6 hours)
- __Players Update__: `0 0 */6 * * *` (every 6 hours)
- __Teams Update__: `0 0 */4 * * *` (every 4 hours)
- __Hero Ranking Update__: `0 0 */2 * * *` (every 2 hours)
- __Aggregations__: `15 5 * * * *` (daily at 05:15)

All these call a shared `runJob(job, description)` helper that builds JobParameters with a `timestamp` to ensure uniqueness.


## Job Definitions (Chunk-Oriented)
Each job is defined with a `JobBuilder` and one `Step` using `StepBuilder`. Steps are chunk-oriented with configured chunk sizes, retry, and skip policies. Common listeners (`BaseJobExecutionListener`, `BaseStepExecutionListener`, and per-step `BaseItemExecutionListener`) are wired into each step/job.

Properties used for chunking, retry, and skip limits are defined in `application.properties` under `app.batch.*`.

### Heroes Update Job
- __Job__: `heroesUpdateJob`
- __Step__: `heroesStep`
- __Reader__: `HeroesReader` (pull heroes from source API)
- __Processor__: `HeroProcessor`
- __Writer__: `HeroWriter`
- __Chunk Size__: `${app.batch.heroes.chunk-size}` (default 10)
- __Retry Limit__: `${app.batch.heroes.retry-limit}` (default 3)
- __Skip Limit__: `${app.batch.heroes.skip-limit}` (default 10)
- __Special__: `preventRestart()` on job

### Teams Update Job
- __Job__: `teamsUpdateJob`
- __Step__: `teamsStep`
- __Reader__: `TeamsReader`
- __Processor__: `TeamProcessor`
- __Writer__: `TeamWriter`
- __Chunk Size__: `${app.batch.teams.chunk-size}` (default 10)
- __Retry Limit__: `${app.batch.teams.retry-limit}` (default 3)
- __Skip Limit__: `${app.batch.teams.skip-limit}` (default 10)

### Notable (Pro) Players Update Job
- __Job__: `proPlayersUpdateJob`
- __Step__: `proPlayersStep`
- __Reader__: `NotablePlayersReader`
- __Processor__: `NotablePlayerProcessor`
- __Writer__: `NotablePlayerWriter`
- __Chunk Size__: `${app.batch.notable-players.chunk-size}` (default 10)
- __Retry Limit__: `${app.batch.notable-players.retry-limit}` (default 3)
- __Skip Limit__: `${app.batch.notable-players.skip-limit}` (default 10)

### Players Update Job
- __Job__: `playerUpdateJob`
- __Step__: `playerStep`
- __Reader__: `PlayerReader`
- __Processor__: `PlayerProcessor`
- __Writer__: `PlayerWriter`
- __Chunk Size__: `${app.batch.players.chunk-size}` (default 100)
- __Retry Limit__: `${app.batch.players.retry-limit}` (default 3)
- __Skip Limit__: `${app.batch.players.skip-limit}` (default 10)

### Hero Rankings Update Job
- __Job__: `heroRankingUpdateJob`
- __Step__: `heroRankingStep`
- __Reader__: `HeroRankingReader` (units of work: hero ids)
- __Processor__: `HeroRankingProcessor` (produces `List<OpenDotaHeroRankingDto>`)
- __Writer__: `HeroRankingWriter`
- __Chunk Size__: `${app.batch.hero-rankings.chunk-size}` (default 10)
- __Retry Limit__: `${app.batch.hero-rankings.retry-limit}` (default 3)
- __Skip Limit__: `${app.batch.hero-rankings.skip-limit}` (default 10)

### New Pro Matches Ingestion Job
- __Job__: `newMatchesIngestionJob`
- __Step__: `newMatchesStep`
- __Reader__: `NewProMatchesReader` (reads new pro matches)
- __Processor__: `ProMatchesToDetailProcessor` (maps basic match rows to detailed match fetch work)
- __Writer__: `MatchDetailWriter` (persists match detail)
- __Chunk Size__: `${app.batch.promatches.new.chunk-size}` (default 10)
- __Fault Tolerance__:
  - Retries `ResourceAccessException` with ExponentialBackOff (1s initial, 10s max, multiplier 2.0), limit 3
  - Skips `HttpClientErrorException` and `CircuitBreakerException` up to limit 50

### Historical Pro Matches Ingestion Job
- __Job__: `historicalMatchesIngestionJob`
- __Step__: `fetchHistoricalMatchesStep`
- __Reader__: `HistoricalProMatchesReader` (reads historical pro matches)
- __Processor__: `ProMatchesToDetailProcessor`
- __Writer__: `MatchDetailWriter`
- __Chunk Size__: `${app.batch.promatches.historical.chunk-size}` (default 20)
- __Fault Tolerance__:
  - Retries `ResourceAccessException` with ExponentialBackOff (1s initial, 10s max, multiplier 2.0), limit 3
  - Skips generic `Exception` up to limit 15


## Aggregations (Non-Batch)
- Orchestrated outside Spring Batch within `BatchSchedulerService#runAggregations()` invoking `AggregationService#refreshPatchesAndAggregations()`.
- Exposed for manual triggering via REST: `POST /api/aggregations/refresh`.
- Scheduled daily at 05:15.


## Data Expiration & Freshness Windows
- `BatchExpirationProperties` (`@ConfigurationProperties(prefix = "app.batch.expiration")`) define per-domain retention/freshness periods:
  - `heroes`, `teams`, `notableplayers`, `herorankings`, `players`, and `defaults`.
- Values are configured in `application.properties`:
  - `app.batch.expiration.heroes=180d`
  - `app.batch.expiration.teams=7d`
  - `app.batch.expiration.notableplayers=3d`
  - `app.batch.expiration.herorankings=3d`
  - `app.batch.expiration.players=14d`
  - `app.batch.expiration.defaults=180d`

These can inform skip/refresh logic in readers/processors and be consulted by higher-level services to decide whether to schedule runs or short-circuit work.


## Error Handling, Resilience and Backoff
- All steps are chunk-oriented and `faultTolerant()` with domain-specific `retry` and `skip` policies.
- Backoff strategies (exponential) are used for network-related failures in matches ingestion.
- A __Circuit Breaker__ layer is present; when open, steps skip work items by throwing/handling `CircuitBreakerException` (see `NewMatchesJobConfig`). The breaker’s status is observable and controllable via REST endpoints:
  - `GET /api/monitoring/circuit-breakers`
  - `POST /api/monitoring/circuit-breakers/{serviceName}/open|close|reset`


## Rate Limiting & Scheduling Strategy
- Scheduling respects daily API quotas via `OpenDotaRateLimitingService` and the `canRunJob()` guard in `BatchSchedulerService`.
- High-frequency jobs (every 15 minutes) handle new data quickly; heavy batch jobs (historical ingestion) run off-peak.
- Aggregations run after ingestion windows so derived data is consistent.


## Observability
- Actuator endpoints are enabled for `health` and `metrics` in `application.properties`:
  - `GET /actuator/health`
  - `GET /actuator/metrics`
- Add more exposure if necessary for batch metrics, e.g., job/step metrics via Micrometer.


## Manual Operations
- Programmatic triggers are available in `BatchSchedulerService` (`trigger*` methods).
- The aggregation can be triggered via REST `POST /api/aggregations/refresh`.
- To change schedules, update the cron expressions in `BatchSchedulerService`.
- To tune throughput and resilience, adjust chunk sizes and `retry/skip` limits in `application.properties`.