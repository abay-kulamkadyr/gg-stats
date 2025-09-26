## Weekly Highlights (Pro Dota) — Full Design & Implementation

### Overview
Weekly Highlights discovers patch-aware trends in professional Dota 2 drafts. It ingests recent pro matches, materializes team lineups, aggregates hero pick rates and hero-combo synergies per patch and per week-in-patch, and exposes an API to query “what’s hot right now.” It is resilient to patch changes and designed for incremental refresh.

### Features 
- Schema extensions for matches, drafts, players, items, and aggregate tables.
- Materialized view that turns picks into team lineups.
- Spring Batch job to ingest pro matches from OpenDota (summaries → details).
- Aggregation services that compute hero trends and hero pair synergy with support/confidence/lift for patch and patch-week buckets.
- Patch metadata ingestion from OpenDota constants.
- HTTP endpoints to manually refresh aggregations and to fetch highlights, with defaults.
---

## Architecture

### Data sources
- OpenDota REST API
  - `/proMatches?less_than_match_id=...` to paginate pro match summaries (100 per page)
  - `/matches/{match_id}` for full match detail (picks/bans, players, purchase logs, etc.)
  - `/constants/patch` for patch id/name/date

### Components
- Ingestion (Spring Batch)
  - `ProMatchesReader`: pulls pages of pro match summaries until exhausted
  - `ProMatchesToDetailProcessor`: extracts match_ids
  - `MatchDetailReader`: fetches `/matches/{id}` JSON details
  - `MatchDetailWriter`: parses details into normalized tables
- Persistence (DAO via JdbcTemplate)
  - `MatchIngestionDao`: upserts into `matches`, `team_match`, `picks_bans`, `draft_timings`, `player_matches`, `item_purchase_event`; refreshes MV
  - `AggregationDao`: refreshes MV, upserts patch constants, runs hero/pair aggregations
  - `HighlightsDao`: reads pre-aggregated highlights (heroes, pairs, counts)
- Services
  - `OpenDotaApiService`: typed client for OpenDota endpoints
  - `AggregationService`: orchestrates patch fetch + MV refresh + aggregations
  - `BatchSchedulerService`: scheduled job runners (hourly ingestion, post-aggregation)
- HTTP Controllers
  - `AggregationsController`: `POST /pro/aggregations/refresh`
  - `HighlightsController`: `GET /pro/highlights`

---

## Database Schema

### Core tables (created in V16__matches_drafts_items.sql)
- `matches` — match metadata: `match_id`, `start_time`, `duration`, `radiant_win`, `leagueid`, `radiant_team_id`, `dire_team_id`, `patch`, `replay_url`, etc.
- `team_match` — links a `team_id` to a `match_id` and side (`radiant` boolean).
- `picks_bans` — one row per draft event: `match_id`, `is_pick`, `hero_id`, `team` (0 radiant, 1 dire), `ord`.
- `draft_timings` — optional timing details per draft order.
- `player_matches` — per-player summary (hero, KDA, items, logs; with generated `is_radiant`).
- `item_purchase_event` — flattened from `purchase_log` for timing analysis.

### Materialized view
- `pro_team_picks_mv` — denormalizes each team’s final 5 picks per match:
  - Columns: `match_id`, `start_time`, `patch`, `team_id`, `is_radiant`, `pick_heroes int[]`
  - Purpose: enable fast lineup-based aggregation without rejoining `picks_bans`

- `pro_hero_item_popularity_mv` — denormalizes items purchase timings for each hero
  - Columns: `hero_id`, `time_bucket`, `item_key`, `purchases`

### Aggregates
- `pro_hero_trends` — per-bucket hero pick rates
- `pro_hero_pair_stats` — per-bucket pair stats: `games_together`, `support`, `confidence`, `lift`

## Ingestion Flow (Spring Batch)

1) Pro match summaries
- `ProMatchesReader` calls OpenDota `/proMatches` repeatedly using `less_than_match_id` to paginate back in time.

2) Match details
- `ProMatchesToDetailProcessor` extracts `match_id` list
- `MatchDetailReader` calls `/matches/{id}` for each id

3) Persist and normalize
- `MatchDetailWriter` ⇒ `MatchIngestionDao` upserts rows to the core tables listed above, and inserts `item_purchase_event` entries

4) Aggregations
- `AggregationService.refreshPatchesAndAggregations()`
  - Fetch `/constants/patch` and upsert into `patch_constants`
  - Refresh MVs
  - Run hero trend and pair synergy aggregations (patch, patch-week)
---

## Aggregation Algorithms

Let a bucket be either:
- Patch: all lineups with `matches.patch = X`
- Patch-week: all lineups with same `patch` and same `epoch_week = floor(start_time/604800)`

From `pro_team_picks_mv` within a bucket:
- `total_lineups` = count of rows
- For each hero h: `hero_lineups(h)` = count of lineups containing h (via `unnest(pick_heroes)`)
- `pick_rate(h)` = `hero_lineups(h) / total_lineups`

For pairs (A,B):
- Generate unordered pairs with ordinality to avoid duplicates: cross-unnest with positions and keep `pos_b > pos_a`
- `games_together(A,B)` = count of lineups containing both A and B
- `support(A,B)` = `games_together / total_lineups`
- `confidence(A→B)` = `games_together / hero_lineups(A)`
- `lift(A,B)` = `support / (pick_rate(A) * pick_rate(B))`

Notes:
- Optional: add `wins_together(A,B)` and `win_rate(h)` by joining lineup side to `matches.radiant_win`
- Recommended thresholds to reduce noise in UI: min bucket size, min hero presence, min pair count

### Step-by-step algorithm with example (how the SQL maps)

Example bucket: patch `58` has 3 team lineups from `pro_team_picks_mv`:

```text
L1 = [84,26,14,11,13]
L2 = [84,110,129,18,49]
L3 = [101,87,14,1,135]
```

1) Total lineups in bucket
- `total_lineups = 3`
- SQL: CTE `totals` does `COUNT(*)` grouped by patch (and by epoch_week for patch-week).

2) Hero pick counts and pick_rate
- Hero 84 appears in L1, L2 → `hero_lineups(84)=2`, `pick_rate(84)=2/3≈0.667`.
- Hero 14 appears in L1, L3 → `hero_lineups(14)=2`, `pick_rate(14)=2/3≈0.667`.
- SQL: CTE `hero_counts` does `unnest(pick_heroes)` and `COUNT(*)` per hero within the bucket; final SELECT computes `pick_rate = hero_lineups / total_lineups`.

3) Pair counts and support/confidence/lift
- Pair (84,14): occurs together only in L1 → `games_together=1`.
- `support = games_together / total_lineups = 1/3≈0.333`.
- `confidence(84→14) = games_together / hero_lineups(84) = 1/2 = 0.5`.
- `lift(84,14) = support / (pick_rate(84)*pick_rate(14)) = (1/3)/((2/3)*(2/3)) ≈ 0.75`.
- SQL: CTE `pairs` generates unordered pairs per lineup using two `unnest(... WITH ORDINALITY)` streams with `b.pos_b > a.pos_a` to avoid duplicates, then groups by `LEAST/GREATEST` of hero ids. Final SELECT joins `totals` and `hero_counts` twice (hA, hB) to compute support, confidence, lift using the same formulas, with `NULLIF` to protect against division by zero.

4) Buckets
- Patch bucket uses `matches.patch` and outputs `bucket_value = patch::text`.
- Patch-week bucket uses `epoch_week = floor(start_time/604800.0)` and outputs `bucket_value = patch||'-'||epoch_week`.
- SQL: `base` CTE differs by including `epoch_week` for patch-week; `totals`/`hero_counts`/`pairs` group by that dimension as well.

5) Upsert
- Results are upserted into `pro_hero_trends` and `pro_hero_pair_stats` with `ON CONFLICT` on the natural keys, updating metrics and `computed_at`.
- Why: keeps aggregations idempotent and incrementally refreshable.

Why this works
- Denominator is total team lineups, not matches, matching the event space where heroes are selected (one lineup per team). This yields meaningful `pick_rate` and ensures pair `support` is comparable.
- `pro_team_picks_mv` guarantees exactly-5 unique picks per team in order, avoiding recomputation from `picks_bans` and enabling efficient `unnest`/pairing.
- `lift` compares observed co-pick rate to independence baseline `p(A)*p(B)`, surfacing synergy (>1) or avoidance (<1).

### Using support, confidence, and lift

- Purpose of each metric
  - **support(A,B)**: prevalence of the pair. Use as a minimum threshold (e.g., at least 10–20 lineups in bucket) and to surface “popular pairs”.
  - **confidence(A→B)**: directional association. If you already picked A, how likely is B. Use for recommendation prompts during draft (“A is usually paired with B”).
  - **lift(A,B)**: synergy vs. chance. Use to surface “surprisingly good” or “meta-defining” pairs. `lift > 1` means together more than chance; the higher, the stronger the association.

- Ranking strategies
  - Top synergy: rank by `lift` with guardrails: `support ≥ min_support` and `hero_lineups(A), hero_lineups(B) ≥ min_hero_presence`.
  - Top popular: rank by `support` to show widely used pairs this bucket.
  - Top conditional: for a given hero A, rank candidates B by `confidence(A→B)` with min-support filter.

- Combined “synergy score” (optional UI metric)
  - Example: `synergy_score = lift * ln(1 + games_together)` to balance effect size and sample size.
  - Or normalize and weight: `z_lift * 0.7 + z_support * 0.3` (within-bucket z-scores).

- Trending highlights (patch-week)
  - Compare to previous week: `delta_support`, `delta_lift`, or percent change. Show pairs with high current `lift` and large positive delta.

- Practical thresholds (tune per data volume)
  - `min_support`: 5–20 (pairs must appear in at least this many lineups in the bucket).
  - `min_hero_presence`: 20–50 lineups for each hero to avoid sparse artifacts.
  - Optionally compute significance (Fisher’s exact test) and require `p < 0.05` when volume is large.

- How the API uses them
  - `GET /pro/highlights` returns pairs primarily ordered for usefulness: either by `lift` with support filters (synergy) or by `support` (popularity). For hero-specific suggestions, filter pairs where `hero_id_a = A` and sort by `confidence(A→B)`.

---

## API

### Refresh Aggregations
POST `/pro/aggregations/refresh`
- Action: fetch patches, refresh MVs, recompute hero trends, pairs, and item purchase timings, for both bucket types

### Highlights (legacy)
GET `/pro/highlights`
- Query params:
  - `bucket` (optional, default=`patch`): `patch` or `patch_week`
  - `value` (optional): bucket identifier; when omitted, the latest bucket is selected automatically
  - `limit` (optional, default=10): number of heroes and pairs to return
  - `sort` (optional, default=`lift` for pairs): one of `lift`, `support`, `confidence`, `games`, `delta_lift`, `delta_support`
  - `offset` (optional, default=0): 0 = latest bucket; 1 = previous week; 2 = two weeks ago, etc. (used when `value` is omitted)
- Response JSON:
  - `matches`: number of team lineups in the bucket
  - `heroes`: array of `{ heroId, matches, picks, pickRate }`
  - `pairs`: array of `{ heroIdA, heroIdB, gamesTogether, support, confidence, lift, deltaSupport, deltaLift }`

---

### Pair Highlights (presets)
GET `/pro/highlights/pairs`

Query params:
- `view` (optional, default=`synergy`): preset that controls sorting
  - `synergy`: sorts by `lift DESC` (best co-pick synergy)
  - `emerging-synergy`: sorts by `delta_lift DESC` (biggest week-over-week lift gains)
  - `trending-popularity`: sorts by `delta_support DESC` (biggest week-over-week support gains)
- `weekOffset` (optional, default=0): 0 current week-in-patch, 1 previous week, etc. Bucket type is fixed to `patch_week`.
- `limit` (optional, default=10): number of pairs to return

Response JSON:
- `bucketValue`: the resolved `patch-week` value used
- `view`: the resolved preset name
- `matches`: number of team lineups in the bucket
- `pairs`: array of `{ heroIdA, heroIdB, gamesTogether, support, confidence, lift, deltaSupport, deltaLift }`

Examples:
- Top synergy this week: `/pro/highlights/pairs` (defaults to `view=synergy`)
- Emerging synergy this week: `/pro/highlights/pairs?view=emerging-synergy`
- Trending popularity last week: `/pro/highlights/pairs?view=trending-popularity&weekOffset=1`

---
