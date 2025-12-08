# Transaction Aggregator

A Spring Boot application that ingests transactions from multiple sources, categorizes them using keyword-based rules, and exposes aggregated transaction data via REST APIs.

## Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Data Sources](#data-sources)

---

## Architecture

### High-Level Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Data Sources Layer                       │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐  │
│  │ Flat File  │  │    XML     │  │  JSON Mock Server    │  │
│  │   Source   │  │   Source   │  │  Random Generator    │  │
│  └──────┬─────┘  └──────┬─────┘  └──────────┬───────────┘  │
└─────────┼────────────────┼───────────────────┼──────────────┘
          │                │                   │
          └────────────────┴───────────────────┘
                           │
                           ▼
          ┌────────────────────────────────────┐
          │   AggregatedTransactionRepository  │
          └────────────────┬───────────────────┘
                           │
                           ▼
          ┌────────────────────────────────────┐
          │     TxCategorizationEngine         │
          │  (Keyword-based categorization)    │
          └────────────────┬───────────────────┘
                           │
                           ▼
          ┌────────────────────────────────────┐
          │     AggregationServiceImpl         │
          │   (Business logic orchestration)   │
          └────────────────┬───────────────────┘
                           │
                           ▼
          ┌────────────────────────────────────┐
          │  AggregatedTransactionController   │
          │         (REST API Layer)           │
          └────────────────────────────────────┘
```

### Core Components

**Data Sources**
- Multiple transaction source implementations: `FlatFileBasedTransactionsSource`, `XmlBasedTransactionsSource`, `JsonMockServerTransactionsSource`, `RandomTransactionsGenerator`
- All implement the `TransactionSource` interface
- Feed into `AggregatedTransactionRepository`

**Domain Models**
- `RawTransaction` - raw transaction data from sources
- `AggregatedTransaction` - processed/enriched transactions with categories
- `TransactionCategory` - categorization metadata
- `CategoryKeyword` - keywords for categorization rules

**Business Logic Layer**
- `TxCategorizationEngine` - handles transaction categorization logic using keyword matching
- `AggregatedTransactionStager` - stages aggregated transactions for querying
- `CategoryDataLoaderService` - loads category definitions and keywords

**Service Layer**
- `AggregationService` - main orchestrator for transaction aggregation
- `AggregationServiceImpl` - concrete implementation

**Controller/API Layer**
- `AggregatedTransactionController` - REST API endpoints
- `AggregatedTransactionResponseMapper` - response transformation
- `TransactionSearch` - search functionality

**Exception Handling**
- Custom exceptions: `InvalidDateRangeException`, `InvalidSearchQueryException`, `CustomerNotFoundException`, `TransactionNotFoundException`
- `GlobalExceptionHandler` - centralized error handling

---

## Prerequisites

- **JDK 21** or higher
- **Maven 3.8+**
- **Docker** and **Docker Compose** (for MySQL and containerized deployment)
- **Git**

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/lizamadubela/capitec-assessment.git
cd transaction-aggregator
```

### 2. Environment Variables

Create a `.env` file in the project root or set the following environment variables:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=data_aggregation
DB_USERNAME=root
DB_PASSWORD=root

```

### 3. Start MySQL with Docker

Run MySQL in a Docker container:

```bash
docker run -d \
  --name mysql-txn-aggregator \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=data_aggregation \
  -e MYSQL_USER=root \
  -e MYSQL_PASSWORD=secure_password \
  -p 3306:3306 \
  mysql:8.0
```

Or use Docker Compose (if you have a `docker-compose.yml`):

```bash
docker-compose up -d mysql
```

---

## Configuration

### Application Configuration (`application.yml`)

Update `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: transaction-aggregator
  
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:data_aggregation}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:secure_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

    liquibase:
    enabled: true
    default-schema: data_aggregation
    change-log: classpath:liquibase/liquibase-changeLog.xml
    liquibase-schema: data_aggregation
    drop-first: true

server:
  port: ${SERVER_PORT:8081}

logging:
  level:
    root: INFO
    za.co.capitecbank.assessment: TRACE
```

### Liquibase Database Migrations

Liquibase will automatically apply database changes on application startup.

**Manual Migration (Optional)**

To apply Liquibase changes manually:

```bash
mvn liquibase:update
```

To rollback the last changeset:

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

---

## Running the Application

### Run Locally (Development Mode)

#### 1. Build the Project

```bash
mvn clean install
```

#### 2. Run the Spring Boot Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/transaction-aggregator-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8081**

#### 3. Run Tests

Run all tests:

```bash
mvn test
```

Run specific test class:

```bash
mvn test -Dtest=AggregatedTransactionControllerTest
```

### Run with Docker

#### 1. Build Docker Image

```bash
docker build -t txn-aggregator:0.0.1 .
```

#### 2. Run Container

```bash
docker run -d \
  --name txn-aggregator \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=transaction_db \
  -e DB_USERNAME=txn_user \
  -e DB_PASSWORD=secure_password \
  txn-aggregator:0.0.1
```

#### 3. Using Docker Compose

If you have a `docker-compose.yml`:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

---

## API Documentation

### Base URL

```
http://localhost:8081/api
```

### Endpoints

#### 1. Get All Transactions for Customer

**Endpoint:** `GET /api/transactions/{customerId}`

**Description:** Retrieves all categorized transactions for a specific customer.

**Path Parameters:**
- `customerId` (String, required) - Customer identifier

**Response:** `200 OK`

```json
[
  {
    "id": 26,
    "customerId": "CUST-1",
    "description": "restaurant",
    "amount": -3243.13,
    "timestamp": "2025-12-04T17:07:25+02:00",
    "source": "POS",
    "category": "Food"
  }
]
```

**Example:**

```bash
curl http://localhost:8081/api/transactions/CUST-1
```

---

#### 2. Get Transactions by Category

**Endpoint:** `GET /api/transactions/{customerId}/categories`

**Description:** Returns transaction totals grouped by category.

**Path Parameters:**
- `customerId` (String, required) - Customer identifier

**Response:** `200 OK`

```json
{
  "Loans": 2748.50,
  "Fuel": 650.00,
  "Income": 3500.00,
  "Shopping": 22615.08,
  "Transport": 47.30,
  "Other": 1729.49,
  "Food": 3799.33
}
```

**Example:**

```bash
curl http://localhost:8081/api/transactions/CUST-1/categories
```

---

#### 3. Get Transactions by Date Range

**Endpoint:** `GET /api/transactions/{customerId}/range`

**Description:** Retrieves transactions within a specified date range.

**Path Parameters:**
- `customerId` (String, required) - Customer identifier

**Query Parameters:**
- `start` (String, required) - Start date (format: `YYYY-MM-DD`)
- `end` (String, required) - End date (format: `YYYY-MM-DD`)

**Response:** `200 OK`

```json
[
  {
    "id": 52,
    "customerId": "CUST-1",
    "description": "uber BV - Ride",
    "amount": 47.30,
    "timestamp": "2025-11-21T19:05:00+02:00",
    "source": "APP",
    "category": "Transport"
  },
  {
    "id": 51,
    "customerId": "CUST-1",
    "description": "Grocery Store - SPAR",
    "amount": 120.55,
    "timestamp": "2025-11-20T10:15:30+02:00",
    "source": "POS",
    "category": "Food"
  }
]
```

**Example:**

```bash
curl "http://localhost:8081/api/transactions/CUST-1/range?start=2025-01-01&end=2025-12-31"
```

---

#### 4. Search Transactions

**Endpoint:** `POST /api/transactions/{customerId}/search`

**Description:** Search transactions by keywords in description or merchant.

**Path Parameters:**
- `customerId` (String, required) - Customer identifier

**Search Parameters:**
**Request**

```json
{
    "searchParameter":"uber"
}

```

**Response:** `200 OK`

```json
[
  {
    "id": 52,
    "customerId": "CUST-1",
    "description": "uber BV - Ride",
    "amount": 47.30,
    "timestamp": "2025-11-21T19:05:00+02:00",
    "source": "APP",
    "category": "Transport"
  }
]
```

**Example:**

```bash
curl "http://localhost:8081/api/transactions/CUST-1/search"
```

---

### Error Responses

All endpoints may return the following error responses:

**404 Not Found**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Customer with ID CUST-999 not found"
}
```

**400 Bad Request**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date range: start date must be before end date"
}
```

---

### Swagger/OpenAPI Documentation

Access interactive API documentation at:

```
http://localhost:8081/swagger-ui.html
```

OpenAPI specification:

```
http://localhost:8081/v3/api-docs
```

---

## Project Structure

```
transaction-aggregator/
├── src/
│   ├── main/
│   │   ├── java/za/co/capitecbank/assessment
│   │   │   ├── config/
│   │   │   │   ├── WebConfig.java
│   │   │   │ 
│   │   │   ├── controller/
│   │   │   │   └── AggregatedTransactionController.java
│   │   │   ├── domain/
│   │   │   │   ├── RawTransaction.java
│   │   │   │   ├── AggregatedTransaction.java
│   │   │   │   ├── TransactionCategory.java
│   │   │   │   └── CategoryKeyword.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── CustomerNotFoundException.java
│   │   │   │   ├── InvalidDateRangeException.java
│   │   │   │   └── TransactionNotFoundException.java
│   │   │   ├── mapper/
│   │   │   │   └── AggregatedTransactionResponseMapper.java
│   │   │   ├── repository/
│   │   │   │   ├── AggregatedTransactionRepository.java
│   │   │   │   ├── TransactionCategoryRepository.java
│   │   │   │   └── CategoryKeywordRepository.java
│   │   │   ├── service/
│   │   │   │   ├── AggregationService.java
│   │   │   │   ├── AggregationServiceImpl.java
│   │   │   │   ├── TxCategorizationEngine.java
│   │   │   │   ├── TxCategorizationEngineImpl.java
│   │   │   │   └── CategoryDataLoaderService.java
│   │   │   ├── source/
│   │   │   │   ├── TransactionSource.java
│   │   │   │   ├── FlatFileBasedTransactionsSource.java
│   │   │   │   ├── XmlBasedTransactionsSource.java
│   │   │   │   ├── JsonMockServerTransactionsSource.java
│   │   │   │   └── RandomTransactionsGenerator.java
│   │   │   └── DataAggregationApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── liquibase/
│   │       │   └── changelog/
│   │       │       ├── liquibase-changeLog.xml
│   │       │       └── db/
│   │       │           ├── 01-create-tables.xml
│   │       │  
│   │       ├── categories.csv
│   │       └── raw-transactions.xml
│   │       ├── raw-fixed-length.txt
│   │       └── transaction-sources.txt
│   │ 
│   └── test/
│       └── java/za/co/capitecbank/assessment
│           ├── controller/
│           │   └── AggregatedTransactionControllerTest.java
│           ├── service/
│           │   ├── AggregationServiceImplTest.java
│           │   └── TxCategorizationEngineImplTest.java
│           └── DataAggregationApplicationTests.java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## Data Sources

### Flat File Format (Fixed-Length Fields)

| Field         | Length | Description                    |
|---------------|--------|--------------------------------|
| transactionId | 12     | Unique transaction identifier  |
| customerId    | 10     | Customer identifier            |
| description   | 30     | Transaction description        |
| merchant      | 20     | Merchant name                  |
| reference     | 15     | Reference number               |
| type          | 6      | Transaction type (DEBIT/CREDIT)|
| amount        | 10     | Transaction amount             |
| currency      | 3      | Currency code (USD, EUR, etc.) |
| timestamp     | 19     | Transaction timestamp          |
| channel       | 10     | Transaction channel (POS, WEB) |
| source        | 10     | Transaction source (CARD, ACH) |
| filler        | 5      | Reserved space                 |

**Example Flat File Record:**

```
TXN-00000001CUST-1    Coffee at Starbucks           Starbucks           REF-123456     DEBIT     4.50      USD2025-01-15T10:30 POS       CARD      00000
```

### XML Format

```xml
<transactions>
  <transaction>
    <transactionId>TXN-001</transactionId>
    <customerId>CUST-1</customerId>
    <description>Coffee purchase</description>
    <merchant>Starbucks</merchant>
    <reference>REF-123456</reference>
    <type>DEBIT</type>
    <amount>4.50</amount>
    <currency>USD</currency>
    <timestamp>2025-01-15T10:30:00</timestamp>
    <channel>POS</channel>
    <source>CARD</source>
  </transaction>
</transactions>
```

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Support

For issues and questions:
- Open an issue on GitHub
- Contact: support@yourcompany.com

---

## Changelog

### Version 0.0.1 (2025-01-15)
- Initial release
- Support for multiple transaction sources
- Keyword-based categorization engine
- REST API for transaction aggregation
- Docker support