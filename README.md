# SetuX API Gateway

SetuX is a learning project for building an API gateway with three Spring Boot services. The current codebase contains starter applications and a `/health` endpoint in each service.

## Services

| Service | Current port | Planned responsibility | Data / messaging in the design |
| --- | ---: | --- | --- |
| Smart Gateway | 8080 | Accept client requests, check policy, and route to backend APIs | Redis for fast policy state; Kafka for access events |
| API Management | 8082 | Manage users, routes, and gateway policies | MySQL for configuration; Kafka for configuration events |
| Analytics | 8081 | Consume access events and provide request statistics | Kafka for events; MongoDB for analytics data |

The ports and responsibilities reflect the current project settings and the supplied architecture image. Most of the business behavior is still planned work.

## Run a service

Each service is an independent Maven/Spring Boot application. Java 21 is configured in its Maven project.

From the service directory, run:

```bash
./mvnw spring-boot:run
```

Then check its health endpoint:

```bash
curl http://localhost:8080/health
curl http://localhost:8082/health
curl http://localhost:8081/health
```

Start one service at a time at first. The database and Kafka dependencies will be needed as those features are implemented; they are not required just to understand the starter health endpoints.

## Suggested learning path

1. Make the three services start reliably and return clear health responses.
2. Build API Management route CRUD and persist routes in MySQL.
3. Have Smart Gateway load route configuration and forward one request to a backend.
4. Add authentication and gateway policies.
5. Publish access events to Kafka and consume them in Analytics.
6. Store and query aggregates in MongoDB.
7. Add Docker Compose, integration checks, and deployment automation.

See [docs/GITHUB_WORKFLOW.md](docs/GITHUB_WORKFLOW.md) for a beginner-friendly Git and GitHub workflow.

## Repository note

Keep this project in its own Git repository. At the time this README was added, the parent directory's Git root was `/Users/shivamdate`, with remote `Spring_Learning.git`; that is broader than this project. Verify the Git root before staging or pushing anything. Do not use `git add .` from the parent directory.
