## Capstone Project — Stage 1 Submission

Author: Abay Kulamkadyr
Date: 2025-08-31

---

---

## High‑Level Overview #1 — GG-Stats — Dota2 Player Insights & Leaderboards
Personalized Dota 2 performance insights with trend analysis and public leaderboards. Pulls match history from OpenDota, computes aggregates, and provides concise views for players and communities.

### Core Use Cases
1) Player Performance Summary for a time range (7/30/90 days)
2) KPI Trends
3) Leaderboards by GPM/XPM/KDA/WinRate with pagination

### Advanced Capabilities
- Counter‑pick and synergy suggestions against enemy lineups
- Per‑hero performance insights and trends for each player
- Match‑level drill‑down with timelines and itemization context
- Optional live match monitoring for real‑time updates
- Optional personalized recommendations with explanations
- Full user accounts/roles (use API keys only)

### Success Criteria
- Fast responses for common queries
- Stable and reproducible leaderboard results
- Clear, documented behavior for edge cases

### Success Metrics
- p95 latency ≤ 200ms (cache) / ≤ 1s (cold)
- Nightly jobs complete with retries; deterministic leaderboards
- Clear, documented APIs with examples
- Leaderboards enforce fairness (minimum matches, deterministic tie‑breakers)


---

## High‑Level Overview #2 — CineScope: Movie Review Platform (OMDb)

A lightweight movie discovery and community reviews platform using the free OMDb API for metadata. Users search titles, build watchlists, and write reviews with ratings.

### Core Use Cases
1) Title search and movie detail views seeded from OMDb
2) User reviews and 1–10 ratings per title
3) Personal watchlists and public top‑rated lists

### Advanced Capabilities
- Basic moderation workflows and anti‑spam safeguards
- Community top lists based on robust scoring
- Optional curated collections and playlists

### Success Criteria
- Useful search and detail pages with accurate metadata
- Trustworthy top lists and visible moderation outcomes
- Clear UX for writing and discovering reviews

### Success Metrics
- p95 latency ≤ 200ms for cached searches
- Clear moderation lifecycle and auditability
- Minimal duplicate titles; predictable IDs (imdb_id)

---

## Task Tracking Framework
- Jira

