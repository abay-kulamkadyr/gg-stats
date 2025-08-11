# Dota 2 Statistics Application

A comprehensive Spring Boot application that fetches and analyzes Dota 2 statistics using the OpenDota API. The application uses Spring Batch for periodic data synchronization while respecting API rate limits.

## Features

- **Heroes Management**: Fetch and store hero information
- **Pro Players Tracking**: Track professional players and their team associations
- **Team Statistics**: Monitor team ratings, wins/losses, and rosters
- **Leaderboard Rankings**: Track top players across different regions
- **Rate-Limited API Integration**: Respects OpenDota API limits (60 requests/min, 2000/day)
- **Scheduled Data Updates**: Automatic periodic updates using Spring Batch
- **REST API**: Full CRUD operations via RESTful endpoints
- **Database Migration**: Flyway database versioning
- **Monitoring**: Spring Boot Actuator for health checks and metrics

## Technology Stack

- **Backend**: Spring Boot 3.5.4, Java 24
- **Database**: PostgreSQL with JPA/Hibernate
- **Batch Processing**: Spring Batch
- **Migration**: Flyway
- **Security**: Spring Security
- **Monitoring**: Spring Boot Actuator
- **Documentation**: OpenAPI/Swagger
- **Containerization**: Docker & Docker Compose

## Database Schema

### Core Tables
- `hero` - Dota 2 heroes with roles and attributes
- `player` - Player profiles and statistics
- `team` - Professional teams and ratings
- `pro_player` - Professional players linked to teams
- `leaderboard_rank` - Current leaderboard positions
- `rank_tier` - Player rank tiers and MMR
- `player_ratings` - Historical rating changes
- `api_rate_limit` - API usage tracking

## Quick Start

### Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd gg-stats
   ```

2. **Start the application**
   ```bash
   docker-compose up -d
   ```

3. **Access the application**
    - API: http://localhost:8080/api
    - Health Check: http://localhost:8080/actuator/health
    - Database: localhost:5432 (postgres/password)

### Manual Setup

#### Prerequisites
- Java 24+
- PostgreSQL 15+
- Maven 3.9+

#### Setup Steps

1. **Database Setup**
   ```sql
   CREATE DATABASE dota_stats;
   CREATE USER dota_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE dota_stats TO dota_user;
   ```

2. **Configure Application**
   ```yaml
   # application.yml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/dota_stats
       username: dota_user
       password: your_password
   ```

3. **Build and Run**
   ```bash
   mvn clean install
   java -jar target/gg-stats-0.0.1-SNAPSHOT.jar
   ```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | Database username | postgres |
| `DB_PASSWORD` | Database password | password |
| `ADMIN_PASSWORD` | Admin interface password | admin123 |

### OpenDota API Configuration

The application is configured with conservative rate limits:
- **Per minute**: 50 requests (API allows 60)
- **Per day**: 1800 requests (API allows 2000)

Modify in `application.yml`:
```yaml
opendota:
  api:
    rate-limit:
      per-minute: 50
      per-day: 1800
```

## API Endpoints

### Batch Operations
- `GET /api/batch/status` - Check API usage and scheduler status
- `POST /api/batch/trigger/heroes` - Manually trigger heroes update
- `POST /api/batch/trigger/pro-players` - Manually trigger pro players update
- `POST /api/batch/trigger/teams` - Manually trigger teams update
- `POST /api/batch/trigger/leaderboard` - Manually trigger leaderboard update

### Data Access
- `GET /api/heroes` - List all heroes (paginated)
- `GET /api/heroes/{id}` - Get specific hero
- `GET /api/players` - List all players (paginated)
- `GET /api/players/{accountId}` - Get specific player
- `GET /api/teams` - List all teams (paginated)
- `GET /api/teams/{teamId}/players` - Get team roster
- `GET /api/leaderboard` - Get leaderboard rankings

## Scheduled Jobs

The application runs automated data updates:

| Job | Schedule | Description |
|-----|----------|-------------|
| Heroes | Daily at 2 AM | Updates hero data (changes infrequently) |
| Pro Players | Every 6 hours | Updates pro player information |
| Teams | Every 4 hours | Updates team ratings and stats |
| Leaderboard | Every 2 hours | Updates player rankings |

## Monitoring

### Health Checks
- Application: `GET /actuator/health`
- Database: `GET /actuator/health/db`
- Batch Jobs: `GET /actuator/batch`

### Metrics
- JVM metrics: `GET /actuator/metrics`
- Custom metrics: `GET /actuator/metrics/{metric-name}`

## Development

### Running Tests
```bash
mvn test
```

### Code Formatting
The project uses Spring Java Format:
```bash
mvn spring-javaformat:apply
```

### Database Migrations
New migrations go in `src/main/resources/db/migration/`:
- Naming: `V{version}__{description}.sql`
- Example: `V2__Add_player_statistics.sql`

### Adding New Batch Jobs

1. **Create Reader, Processor, Writer**
   ```java
   @Component
   public class CustomReader implements ItemReader<JsonNode> {
       // Implementation
   }
   ```

2. **Configure Job in BatchConfiguration**
   ```java
   @Bean
   public Job customUpdateJob(Step customStep) {
       return new JobBuilder("customUpdateJob", jobRepository)
               .start(customStep)
               .build();
   }
   ```

3. **Add Scheduler**
   ```java
   @Scheduled(cron = "0 0 */1 * * *")
   public void runCustomUpdateJob() {
       if (canRunJob()) {
           runJob(customUpdateJob, "Custom Update");
       }
   }
   ```

## Troubleshooting

### Common Issues

1. **Rate Limit Exceeded**
    - Check `/api/batch/status` for remaining requests
    - Jobs will automatically skip if limits are reached
    - Consider increasing delays between requests

2. **Database Connection Issues**
    - Verify PostgreSQL is running
    - Check connection credentials
    - Ensure database exists

3. **Batch Job Failures**
    - Check application logs
    - Verify OpenDota API is accessible
    - Check data format changes in API responses

### Logs
- Application logs: `logs/gg-stats.log`
- Docker logs: `docker-compose logs app`
- Batch job status: Available via actuator endpoints

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make changes and add tests
4. Ensure code formatting: `mvn spring-javaformat:apply`
5. Run tests: `mvn test`
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [OpenDota](https://www.opendota.com/) for providing the free API