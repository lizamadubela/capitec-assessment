# Transaction Aggregator

A Spring Boot application that ingests transactions from multiple sources, categorizes them using keyword-based rules, and exposes aggregated transaction data via REST APIs.

## Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [MockServer Setup](#mockserver-setup)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Data Sources](#data-sources)

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

## Prerequisites

- **JDK 21** or higher
- **Maven 3.8+**
- **Docker** and **Docker Compose** (for MySQL and containerized deployment)
- **Git**

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/lizamadubela/capitec-assessment.git
cd transaction-aggregator
```

### 2. Project Structure

Ensure your project has the following structure for Docker setup:

```
transaction-aggregator/
├── docker/
│   ├── mysql/
│   │   └── init/           # MySQL initialization scripts (optional)
│   └── mockserver/
│       └── config/
│           └── transactions.json    # MockServer expectations
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── src/
```

## Configuration

### Application Configuration

Update `src/main/resources/application.yml` or `application.properties`:

**application.yml:**

```yaml
server:
  port: 8081

spring:
  jackson:
    date-format: "yyyy-MM-dd HH:mm:ss"
    serialization:
      write-dates-as-timestamps: false

  datasource:
    url: jdbc:mysql://mysql:3306/data_aggregation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: none   # Liquibase handles schema

  liquibase:
    enabled: true
    default-schema: data_aggregation
    change-log: classpath:liquibase/changelog/liquibase-changeLog.xml
    liquibase-schema: data_aggregation
    drop-first: true

main:
  allow-bean-definition-overriding: true

app:
  xml-data-file: classpath:raw-transactions.xml
  category-data: classpath:categories.csv
  flat-file: classpath:raw-fixed-length.txt
  # Use the mock-server service name so the app reaches it inside the network
  json-server:
    base-url: http://mock-server:1080
```

### Docker Compose Configuration

Create or update `docker-compose.yml`:

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: mysql-data-aggregation
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: data_aggregation
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d
    networks:
      - txn-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: data-aggregation-app:1.0.7
    container_name: txn-aggregator
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/data_aggregation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: com.mysql.cj.jdbc.Driver
      SERVER_PORT: 8081
      JAVA_OPTS: >-
        -Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    ports:
      - "8081:8081"
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    networks:
      - txn-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  mock-server:
    image: mockserver/mockserver:latest
    container_name: mock-txn-server
    restart: unless-stopped
    ports:
      - "1080:1080"
    environment:
      MOCKSERVER_INITIALIZATION_JSON_PATH: /config/transactions.json
    volumes:
      - ./docker/mockserver/config:/config
    networks:
      - txn-network

volumes:
  mysql_data:

networks:
  txn-network:
    driver: bridge
```

### Dockerfile

Create `Dockerfile` in the project root:

```dockerfile
# Use a base image with a Java Runtime Environment (JRE)
FROM amazoncorretto:21-alpine-jdk

# Setting the working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY target/transaction-aggregator-0.0.1-SNAPSHOT.jar data-aggregation-app.jar

# Expose the port
EXPOSE 8081

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "data-aggregation-app.jar"]

```
## Running the Application

### Run with Docker Compose (Recommended)

This will start MySQL, the application, and MockServer together:

```bash
# Start all services
docker-compose up -d

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f app
docker-compose logs -f mysql
docker-compose logs -f mock-server

# Check service status
docker-compose ps

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### Option 2: Run Locally (Development Mode)

#### 1. Start MySQL

```bash
docker-compose up -d mysql
```

#### 2. Start MockServer

```bash
docker-compose up -d mock-server
```

#### 3. Build the Project

```bash
mvn clean install
```

#### 4. Run the Spring Boot Application

```bash
# Using Maven
mvn spring-boot:run

# Or run the JAR directly
java -jar target/transaction-aggregator-0.0.1-SNAPSHOT.jar

# With custom properties
java -jar target/transaction-aggregator-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/data_aggregation \
  --app.json-server.base-url=http://localhost:1080
```

The application will start on **http://localhost:8081**

### Option 3: Build and Run Docker Image Manually

```bash
# Build the Docker image
docker build -t txn-aggregator:1.0.0 .

# Run the container
docker run -d \
  --name txn-aggregator \
  --network txn-network \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/data_aggregation \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e MOCK_SERVER_URL=http://mock-txn-server:1080 \
  txn-aggregator:1.0.0

# View logs
docker logs -f txn-aggregator
```

### Verify Services are Running

```bash
# Check application health
curl http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP"}

# Check MySQL
docker exec -it mysql-data-aggregation mysql -uroot -proot -e "SHOW DATABASES;"
```

### Troubleshooting

#### Application Won't Start

```bash
# Check if MySQL is ready
docker logs mysql-data-aggregation

# Check application logs
docker logs txn-aggregator
```

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

#### 3. Get Transactions by Date Range

**Endpoint:** `GET /api/transactions/{customerId}/range`

**Description:** Retrieves transactions within a specified date range.

**Path Parameters:**
- `customerId` (String, required) - Customer identifier

**Query Parameters:**
- `start` (String, required) - Start date (format: YYYY-MM-DD)
- `end` (String, required) - End date (format: YYYY-MM-DD)

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
curl "http://localhost:8081/api/transactions/CUST-1/range?start=2025-01-01&end=2025-12-31"
```

#### 4. Search Transactions

**Endpoint:** `POST /api/transactions/{customerId}/search`

**Description:** Search transactions by keywords in description or merchant.

**Path Parameters:**
- `customerId` (String, required) - Customer identifier

**Request Body:**

```json
{
  "searchParameter": "uber"
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
curl -X POST http://localhost:8081/api/transactions/CUST-1/search \
  -H "Content-Type: application/json" \
  -d '{"searchParameter":"uber"}'
```

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

### Swagger/OpenAPI Documentation

Access interactive API documentation at:

- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **OpenAPI Spec:** http://localhost:8081/v3/api-docs

## Project Structure

```
transaction-aggregator/
├── docker/
│   ├── mysql/
│   │   └── init/
│   └── mockserver/
│       └── config/
│           └── transactions.json
├── src/
│   ├── main/
│   │   ├── java/za/co/capitecbank/assessment/
│   │   │   ├── config/
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/
│   │   │   │   └── AggregatedTransactionController.java
│   │   │   ├── domain/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── RawTransaction.java
│   │   │   │   │   ├── AggregatedTransaction.java
│   │   │   │   │   ├── TransactionCategory.java
│   │   │   │   │   └── CategoryKeyword.java
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
│   │   │   │   └── CategoryDataLoaderService.java
│   │   │   ├── transactions/
│   │   │   │   └── source/
│   │   │   │       ├── TransactionSource.java
│   │   │   │       └── impl/
│   │   │   │           ├── FlatFileBasedTransactionsSource.java
│   │   │   │           ├── XmlBasedTransactionsSource.java
│   │   │   │           ├── JsonMockServerTransactionsSource.java
│   │   │   │           └── RandomTransactionsGenerator.java
│   │   │   └── DataAggregationApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── liquibase/
│   │       │   └── changelog/
│   │       │       ├── liquibase-changeLog.xml
│   │       │       └── changes/
│   │       │           └── 01-create-tables.xml
│   │       ├── categories.csv
│   │       ├── raw-transactions.xml
│   │       └── raw-fixed-length.txt
│   └── test/
│       └── java/za/co/capitecbank/assessment/
│           ├── controller/
│           │   └── AggregatedTransactionControllerTest.java
│           ├── service/
│           │   └── CategoryDataLoaderServiceTest.java
│           └── DataAggregationApplicationTests.java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Data Sources

### Flat File Format (Fixed-Length Fields)

| Field             | Length | Description                        |
|-------------------|--------|------------------------------------|
| transactionId     | 12     | Unique transaction identifier      |
| customerId        | 10     | Customer identifier                |
| description       | 30     | Transaction description            |
| merchant          | 20     | Merchant name                      |
| reference         | 15     | Reference number                   |
| type              | 6      | Transaction type (DEBIT/CREDIT)    |
| amount            | 10     | Transaction amount                 |
| currency          | 3      | Currency code (USD, EUR, etc.)     |
| timestamp         | 19     | Transaction timestamp              |
| channel           | 10     | Transaction channel (POS, WEB)     |
| source            | 10     | Transaction source (CARD, ACH)     |
| filler            | 5      | Reserved space                     |

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

### JSON MockServer Format

The MockServer provides transactions in JSON format via REST API:

```json
{
  "transactions": [
    {
      "transactionId": "tx-55",
      "customerId": "CUST-2",
      "accountId": "ACCT-2001",
      "description": "Coffee Shop Purchase",
      "merchant": "Local Coffee Shop",
      "mcc": 5814,
      "amount": 48.00,
      "currency": "ZAR",
      "type": "DEBIT",
      "status": "POSTED",
      "timestamp": "2025-12-06T10:15:20",
      "source": "Scan To Pay"
    }
  ]
}
```