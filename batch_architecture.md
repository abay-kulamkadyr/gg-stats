# GG Stats - Dota 2 Statistics Batch Processing System

A robust Spring Boot application for collecting, processing, and storing Dota 2 statistics from the OpenDota API using Spring Batch with comprehensive resilience patterns.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [System Components](#system-components)
- [Data Flow](#data-flow)
- [Batch Processing](#batch-processing)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Overview

GG Stats is a production-ready batch processing system that:
- **Collects data** from OpenDota API with rate limiting and circuit breaker protection
- **Processes statistics** for heroes, players, teams, and rankings
- **Stores data** in PostgreSQL with proper transaction management
- **Provides monitoring** through structured logging and metrics collection
- **Ensures resilience** through comprehensive error handling and recovery mechanisms

## Architecture

### High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           GG Stats Application                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐            │
│  │   Web Layer    │    │  Batch Layer    │    │  Service Layer  │            │
│  │                 │    │                 │    │                 │            │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │            │
│  │ │Controllers  │ │    │ │Job Configs  │ │    │ │API Services │ │            │
│  │ │             │ │    │ │             │ │    │ │             │            │
│  │ │Monitoring   │ │    │ │Step Configs │ │    │ │Circuit      │ │            │
│  │ │Controller   │ │    │ │             │ │    │ │Breaker      │ │            │
│  └─┴─────────────┴─┘    │ ┌─────────────┐ │    │ ┌─────────────┐ │            │
│                          │ │Readers      │ │    │ │Rate         │ │            │
│                          │ │Processors   │ │    │ │Limiting     │ │            │
│                          │ │Writers      │ │    │ │             │ │            │
│                          │ └─────────────┘ │    │ ┌─────────────┐ │            │
│                          └─────────────────┘    │ │Metrics      │ │            │
│                                                  │ │Service      │ │            │
│                                                  └─┴─────────────┴─┘            │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Detailed Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    GG Stats Codebase Structure                                   │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Application Entry Points                                      │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │ GgStatsApplication │  │ StartupJobRunner    │  │ ApplicationStartup  │                │ │
│  │  │ (Main Class)       │  │ (ApplicationRunner) │  │ Listener            │                │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Configuration Layer                                            │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │ BatchConfiguration │  │ AsyncConfig         │  │ SecurityConfig      │                │ │
│  │  │ (Job Repository)   │  │ (Task Executors)    │  │ (Basic Auth)        │                │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │ HeroesBatchConfig   │  │ PlayersBatchConfig  │  │ TeamsBatchConfig    │                │ │
│  │  │ NotablePlayersBatch │  │ HeroRankingsBatch   │  │ (Individual Jobs)   │                │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    BatchExpirationConfig                                               │ │ │
│  │  │              (Data Expiration Management)                                              │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Service Layer                                                 │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │ BatchScheduler     │  │ OpenDotaApiService  │  │ CircuitBreaker      │                │ │
│  │  │ (Job Scheduling)   │  │ (External API)      │  │ Service             │                │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │ RateLimiting       │  │ MetricsService      │  │ ServiceLogger       │                │ │
│  │  │ Service            │  │ (Performance)       │  │ (Service Logging)   │                │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    ConfigurationService                                                 │ │ │
│  │  │                  (App Configuration)                                                    │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Batch Processing Layer                                        │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                           Base Classes                                                  │ │ │
│  │  │                                                                                         │ │ │
│  │  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                        │ │ │
│  │  │  │ BaseApiReader   │  │ BaseProcessor   │  │ BaseWriter      │                        │ │ │
│  │  │  │ (API Reading)   │  │ (Data Process)  │  │ (DB Writing)    │                        │ │ │
│  │  │  └─────────────────┘  └─────────────────┘  └─────────────────┘                        │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                        Concrete Implementations                                        │ │ │
│  │  │                                                                                         │ │ │
│  │  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                        │ │ │
│  │  │  │ HeroesReader    │  │ HeroesProcessor │  │ HeroesWriter    │                        │ │ │
│  │  │  │ PlayersReader   │  │ PlayersProcessor│  │ PlayersWriter   │                        │ │ │
│  │  │  │ TeamsReader     │  │ TeamsProcessor  │  │ TeamsWriter     │                        │ │ │
│  │  │  │ NotablePlayers  │  │ NotablePlayers  │  │ NotablePlayers  │                        │ │ │
│  │  │  │ Reader          │  │ Processor       │  │ Writer          │                        │ │ │
│  │  │  │ HeroRanking     │  │ HeroRanking     │  │ HeroRanking     │                        │ │ │
│  │  │  │ Reader          │  │ Processor       │  │ Writer          │                        │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                           Execution Listeners                                           │ │ │
│  │  │                                                                                         │ │ │
│  │  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                        │ │ │
│  │  │  │ BaseJobExec     │  │ BaseStepExec    │  │ Specific       │                        │ │ │
│  │  │  │ Listener        │  │ Listener        │  │ Listeners      │                        │ │ │
│  │  │  │ (Job Lifecycle) │  │ (Step Lifecycle)│  │ (Heroes, etc.) │                        │ │ │
│  │  │  └─────────────────┘  └─────────────────┘  └─────────────────┘                        │ │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Data Layer                                                    │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │      Entities       │  │    Repositories     │  │   Database          │                │ │
│  │  │                     │  │                     │  │                     │                │ │
│  │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │                │ │
│  │  │ │ Hero            │ │  │ │ HeroRepository  │ │  │ │ PostgreSQL      │ │                │ │
│  │  │ │ Player          │ │  │ │ PlayerRepository│ │  │ │ (Dota Stats DB) │ │                │ │
│  │  │ │ Team            │ │  │ │ TeamRepository  │ │  │ │                 │ │                │ │
│  │  │ │ NotablePlayer   │ │  │ │ NotablePlayer   │ │  │ │ Batch Tables    │ │                │ │
│  │  │ │ HeroRanking     │ │  │ │ Repository      │ │  │ │ (BATCH_*)       │ │                │ │
│  │  │ │ PlayerRatings   │ │  │ │ HeroRanking     │ │  │ │                 │ │                │ │
│  │  │ │ ApiRateLimit    │ │  │ │ Repository      │ │  │ │ Migration Files │ │                │ │
│  │  └─┴─────────────────┴─┘  │ └─────────────────┘ │  │ └─────────────────┘ │                │ │
│  │                           │                     │                     │                │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    Database Migrations (V1-V13)                                        │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Utility Layer                                                 │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                │ │
│  │  │ LoggingUtils        │  │ MDCLoggingContext   │  │ LoggingConstants    │                │ │
│  │  │ (Structured Log)    │  │ (MDC Management)    │  │ (Log Constants)     │                │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                │ │
│  │                                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    Exception Hierarchy                                                 │ │ │
│  │  │                                                                                         │ │ │
│  │  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                        │ │ │
│  │  │  │ ApiService      │  │ CircuitBreaker  │  │ RateLimit       │                        │ │ │
│  │  │  │ Exception       │  │ Exception       │  │ Exceeded        │                        │ │ │
│  │  │  │                 │  │                 │  │ Exception       │                        │ │ │
│  │  │  │ Configuration   │  │ CircuitBreaker  │  │                 │                        │ │ │
│  │  │  │ Exception       │  │ OpenException   │  │                 │                        │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## System Components

### Data Flow Diagram

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Scheduled     │    │   Batch Job     │    │   API Service   │    │   Database      │
│   Trigger       │───▶│   Execution     │───▶│   (OpenDota)   │───▶│   Storage      │
│                 │    │                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │                       ▼                       ▼                       ▼
         │              ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
         │              │   Rate Limit    │    │   Circuit       │    │   Entity        │
         │              │   Check         │    │   Breaker       │    │   Processing    │
         │              └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │                       ▼                       ▼                       ▼
         │              ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
         │              │   Data          │    │   Error         │    │   Repository    │
         │              │   Validation    │    │   Handling      │    │   Operations    │
         └──────────────┴─────────────────┴────┴─────────────────┴────┴─────────────────┘
```

### Service Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Service Dependency Graph                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐                                                           │
│  │ BatchScheduler  │                                                           │
│  │ Service         │                                                           │
│  └─────────┬───────┘                                                           │
│            │                                                                   │
│            ▼                                                                   │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐            │
│  │ OpenDotaApi     │───▶│ CircuitBreaker  │───▶│ RateLimiting    │            │
│  │ Service         │    │ Service         │    │ Service         │            │
│  └─────────┬───────┘    └─────────────────┘    └─────────────────┘            │
│            │                                                                   │
│            ▼                                                                   │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐            │
│  │ MetricsService  │    │ ServiceLogger   │    │ Configuration   │            │
│  │ (Performance)   │    │ (Logging)       │    │ Service         │            │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘            │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Batch Components                              │ │
│  │                                                                             │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐            │ │
│  │  │ BaseApiReader   │──│ BaseProcessor   │──│ BaseWriter      │            │ │
│  │  │ (Inheritance)   │  │ (Inheritance)   │  │ (Inheritance)   │            │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘            │ │
│  │                                                                             │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐            │ │
│  │  │ Job Listeners   │  │ Step Listeners  │  │ Batch Configs   │            │ │
│  │  │ (Lifecycle)     │  │ (Lifecycle)     │  │ (Job/Step Def)  │            │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘            │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Batch Processing

### Batch Job Execution Flow

```
┌─────────────────┐
│   Job Start     │
│   (Listener)    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Step 1:      │───▶│   Step 2:       │───▶│   Step 3:       │
│   Read Data    │    │   Process Data  │    │   Write Data    │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ API Reader  │ │    │ │ Processor   │ │    │ │ Writer      │ │
│ │ Rate Limit  │ │    │ │ Validation  │ │    │ │ Database    │ │
│ │ Circuit     │ │    │ │ Transform   │ │    │ │ Transaction │ │
│ │ Breaker     │ │    │ │ Business    │ │    │ │ Batch       │ │
│ └─────────────┘ │    │ │ Logic       │ │    │ │ Operations  │ │
└─────────────────┘    └─────────────┴─┘    └─────────────┴─┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Logging       │    │   Logging       │    │   Logging       │
│   Context:      │    │   Context:      │    │   Context:      │
│   - correlationId│    │   - stepName    │    │   - batchType   │
│   - jobId       │    │   - batchType   │    │   - operationType│
│   - operationType│    │   - operationType│    │   - success/fail│
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Configuration Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Configuration Hierarchy                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐                                                           │
│  │ application.properties (Root Config)                                        │
│  │ - Database settings                                                         │
│  │ - API endpoints                                                             │
│  │ - Rate limiting                                                             │
│  │ - Circuit breaker                                                           │
│  └─────────────────┘                                                           │
│            │                                                                   │
│            ▼                                                                   │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐            │
│  │ BatchConfig     │    │ AsyncConfig     │    │ SecurityConfig  │            │
│  │ (Job Repository)│    │ (Task Executors)│    │ (Authentication)│            │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘            │
│            │                                                                   │
│            ▼                                                                   │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐            │
│  │ HeroesBatch     │    │ PlayersBatch    │    │ TeamsBatch      │            │
│  │ NotablePlayers  │    │ HeroRankings    │    │ (Individual     │            │
│  │ (Job Configs)   │    │ (Job Configs)   │    │  Job Configs)   │            │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘            │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                              Environment-Specific                          │ │
│  │                                                                             │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐            │ │
│  │  │ Development     │  │ Production      │  │ Test            │            │ │
│  │  │ (logback-spring│  │ (logback-spring│  │ (application-   │            │ │
│  │  │  .xml)         │  │  .xml)         │  │  test.properties│            │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘            │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Design Patterns Used                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Template Method │  │ Strategy        │  │ Observer        │                │
│  │ (Base Classes)  │  │ (Different      │  │ (Listeners)     │                │
│  │                 │  │  Implementations│  │                 │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Circuit Breaker │  │ Rate Limiting   │  │ Factory         │                │
│  │ (Resilience)    │  │ (API Protection)│  │ (Object Creation│                │
│  │                 │  │                 │  │                 │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │ Repository      │  │ Builder         │  │ Decorator       │                │
│  │ (Data Access)   │  │ (Configuration) │  │ (Logging)       │                │
│  │                 │  │                 │  │                 │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Getting Started

### Installation

1. **Configure database**
   ```bash
   # Update application.properties with your database credentials
   spring.datasource.url=jdbc:postgresql://localhost:5432/dota_stats
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

2. **Run***
   ```bash
   # Flyway will automatically run migrations on startup
   mvn spring-boot:run
   ```

### Configuration

#### Profile-Specific Configuration

- **Development**: `spring.profiles.active=dev`
- **Production**: `spring.profiles.active=prod`
- **Test**: `spring.profiles.active=test`

## Monitoring

### Logging

The application uses structured logging with MDC context:

```json
{
  "@timestamp": "2025-08-29T19:37:02.464006895+05:00",
  "message": "✅ API response from /rankings?hero_id=25: status=200, time=540ms, size=21835bytes",
  "correlationId": "81ee525e-7562-4fb6-b75f-01835563542f",
  "jobId": "282",
  "stepName": "heroRankingStep",
  "operationType": "API_CALL",
  "batchType": "herorankings",
  "apiEndpoint": "/rankings?hero_id=25"
}
```

### Metrics

- **API Response Times**: Average, min, max
- **Batch Processing Rates**: Items per second
- **Circuit Breaker Status**: Open/Closed/Half-Open
- **Rate Limiting**: Permits used, remaining

### Health Checks

- **Database Connectivity**: Connection pool status
- **API Health**: OpenDota API availability
- **Batch Job Status**: Job execution history
- **System Resources**: Memory, CPU usage

## Troubleshooting

### Common Issues

1. **Rate Limit Exceeded**
   ```
   Rate limit exceeded - endpoint: /rankings, reason: PER_MINUTE_LIMIT
   ```
   **Solution**: Wait for rate limit reset or adjust configuration

2. **Circuit Breaker Open**
   ```
   Circuit breaker is open, using fallback
   ```
   **Solution**: Check API availability and circuit breaker configuration

3. **Database Connection Issues**
   ```
   Could not create connection to database server
   ```
   **Solution**: Verify database credentials and connectivity

### Debug Mode

Enable debug logging:

```properties
logging.level.com.abe.gg_stats=DEBUG
logging.level.org.springframework.batch=DEBUG
```

### Performance Tuning

1. **Adjust chunk size** based on memory constraints
2. **Configure connection pool** for database performance
3. **Tune rate limiting** based on API quotas
4. **Optimize batch job scheduling** for your use case

## API Documentation

### Batch Job Endpoints

- `POST /api/batch/heroes/trigger` - Trigger heroes update job
- `POST /api/batch/players/trigger` - Trigger players update job
- `POST /api/batch/teams/trigger` - Trigger teams update job
- `POST /api/batch/rankings/trigger` - Trigger hero rankings update job

### Monitoring Endpoints

- `GET /actuator/health` - Application health status
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/batch-jobs` - Batch job execution history

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**GG Stats** - Built with using Spring Boot, Spring Batch, and modern resilience patterns.
