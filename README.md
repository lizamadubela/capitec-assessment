# Transaction Aggregator (Spring Boot, Java 21)
Minimal demo service that aggregates transaction records from multiple mock
providers, categorizes them and exposes aggregation APIs.
## Requirements
- JDK 21
- Maven 3.8+
- Docker (optional, for container run)
## Build and run locally (without Docker)
1. Build:
```bash
mvn clean package -DskipTests
Run:
java -jar target/transaction-aggregator-0.0.1-SNAPSHOT.jar
The service will start on port 8080 .
Build and run with Docker
Build the image:
docker build -t txn-aggregator:0.0.1 .
Run the container:
1.
1.
1.
12
docker run -p 8080:8080 --name txn-aggregator txn-aggregator:0.0.1
Endpoints
GET /api/aggregatedTransactions/{customerId} — all aggregatedTransactions (categorized).
GET /api/aggregatedTransactions/{customerId}/categories — totals grouped by category.
GET /api/aggregatedTransactions/{customerId}/range?start=2025-01-01&end=2025-12-31 —
aggregatedTransactions in provided date range.
Example:
curl http://localhost:8080/api/aggregatedTransactions/CUST-1
curl http://localhost:8080/api/aggregatedTransactions/CUST-1/categories
curl "http://localhost:8080/api/aggregatedTransactions/CUST-1/range?
start=2025-01-01&end=2025-12-31"


Flat file fixed length fileds

| Field         | Length |
| ------------- | ------ |
| transactionId | 12     |
| customerId    | 10     |
| description   | 30     |
| merchant      | 20     |
| reference     | 15     |
| type          | 6      |
| amount        | 10     |
| currency      | 3      |
| timestamp     | 19     |
| channel       | 10     |
| source        | 10     |
| filler        | 5      |
