# High-Level Design (HLD) - Complete Step-by-Step Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Purpose of HLD](#purpose-of-hld)
3. [Step-by-Step HLD Process](#step-by-step-hld-process)
4. [HLD Components](#hld-components)
5. [Best Practices](#best-practices)
6. [Example Template](#example-template)

---

## Introduction

High-Level Design (HLD) is an architectural blueprint that provides a bird's-eye view of the entire system. It focuses on the system architecture, major components, their interactions, and technology choices without diving into implementation details.

**Target Audience:** Architects, Senior Developers, Project Managers, Stakeholders

---

## Purpose of HLD

- **System Overview:** Understand the complete system architecture
- **Component Identification:** Identify major modules and their responsibilities
- **Technology Stack:** Define technologies, frameworks, and tools
- **Integration Points:** Show how different components communicate
- **Scalability & Performance:** Address non-functional requirements
- **Risk Assessment:** Identify potential bottlenecks and challenges

---

## Step-by-Step HLD Process

### Step 1: Understand Requirements
**What to do:**
- Gather functional requirements (what the system should do)
- Gather non-functional requirements (performance, scalability, security, availability)
- Identify stakeholders and their expectations
- Understand business goals and constraints

**Deliverable:**
- Requirements document summary
- Use cases or user stories
- Success criteria

---

### Step 2: Define System Scope
**What to do:**
- Define what is IN scope and OUT of scope
- Identify system boundaries
- List assumptions and constraints
- Define success metrics

**Deliverable:**
- Scope statement
- Assumptions list
- Constraints list

---

### Step 3: Identify Major Components/Modules
**What to do:**
- Break down the system into logical components
- Define responsibility of each component
- Identify core services, databases, APIs, UI layers
- Group related functionalities

**Example Components:**
- Frontend (Web/Mobile)
- Backend Services (API Gateway, Microservices)
- Database Layer (SQL/NoSQL)
- Cache Layer (Redis, Memcached)
- Message Queue (Kafka, RabbitMQ)
- Authentication & Authorization
- External Integrations

**Deliverable:**
- Component diagram
- Component responsibility matrix

---

### Step 4: Design System Architecture
**What to do:**
- Choose architectural pattern (Monolithic, Microservices, Serverless, Event-Driven)
- Design data flow between components
- Define communication protocols (REST, gRPC, GraphQL, WebSockets)
- Plan deployment architecture (Cloud, On-premise, Hybrid)

**Deliverable:**
- Architecture diagram (use tools like Draw.io, Lucidchart, PlantUML)
- Data flow diagrams
- Sequence diagrams for critical flows

---

### Step 5: Select Technology Stack
**What to do:**
- Choose programming languages
- Select frameworks and libraries
- Choose database technologies
- Select cloud provider (AWS, Azure, GCP) or on-premise
- Choose DevOps tools (CI/CD, monitoring, logging)

**Important:** If you're new to any technology, refer to `Technology_Stack_Glossary.md` for detailed explanations of each technology, what it does, and when to use it.

**Example Technology Stack with Explanations:**

**Frontend:**
- **React.js** - JavaScript library for building UIs with reusable components
  - *Why:* Large ecosystem, excellent performance, strong community
- **TypeScript** - JavaScript with type checking
  - *Why:* Catches errors early, better IDE support
- **Redux Toolkit** - State management for React
  - *Why:* Predictable state updates, debugging tools

**Backend:**
- **Node.js + Express** - JavaScript runtime and web framework
  - *Why:* Fast I/O, same language as frontend, large npm ecosystem
  - *Use case:* API Gateway, real-time services
- **Java + Spring Boot** - Enterprise framework
  - *Why:* Strong typing, mature ecosystem, production-ready
  - *Use case:* Core business logic, high-throughput services
- **Python + FastAPI** - Modern Python framework
  - *Why:* Easy syntax, auto documentation, async support
  - *Use case:* Data processing, ML integration

**Databases:**
- **PostgreSQL** - Relational database
  - *What it is:* Advanced SQL database with ACID compliance
  - *Why:* Strong data integrity, complex queries, JSON support
  - *Use case:* User data, orders, transactions
- **MongoDB** - NoSQL document database
  - *What it is:* Stores data in JSON-like documents
  - *Why:* Flexible schema, horizontal scaling
  - *Use case:* Product catalog, user preferences
- **Redis** - In-memory cache
  - *What it is:* Ultra-fast key-value store
  - *Why:* Sub-millisecond latency, data structures
  - *Use case:* Session storage, caching, rate limiting

**Infrastructure:**
- **AWS** - Cloud provider
  - *What it is:* Leading cloud platform with 200+ services
  - *Why:* Reliability, global presence, comprehensive services
  - *Key services:* EC2 (servers), RDS (databases), S3 (storage)
- **Docker** - Containerization
  - *What it is:* Package apps with dependencies
  - *Why:* Consistent environments, easy deployment
- **Kubernetes (EKS)** - Container orchestration
  - *What it is:* Automates deployment and scaling of containers
  - *Why:* Auto-scaling, self-healing, rolling updates
- **API Gateway (Kong)** - Request routing
  - *What it is:* Entry point for all API requests
  - *Why:* Centralized auth, rate limiting, routing
- **Load Balancer (AWS ALB)** - Traffic distribution
  - *What it is:* Distributes requests across multiple servers
  - *Why:* High availability, health checks
- **CDN (CloudFront)** - Content delivery
  - *What it is:* Caches content at edge locations globally
  - *Why:* Faster delivery, reduced server load
- **DNS (Route53)** - Domain management
  - *What it is:* Translates domain names to IP addresses
  - *Why:* Health checks, failover, geo-routing

**Message Queue:**
- **Apache Kafka** - Event streaming
  - *What it is:* Distributed platform for real-time data pipelines
  - *Why:* High throughput, fault-tolerant, message replay
  - *Use case:* Order events, notifications, async processing

**Monitoring & Logging:**
- **Prometheus + Grafana** - Metrics and dashboards
  - *Prometheus:* Collects and stores metrics
  - *Grafana:* Visualizes metrics in dashboards
  - *Why:* Open-source, powerful querying
- **ELK Stack** - Centralized logging
  - *Elasticsearch:* Stores and searches logs
  - *Logstash:* Collects and transforms logs
  - *Kibana:* Visualizes logs
  - *Why:* Centralized logging, powerful search

**CI/CD:**
- **GitHub Actions** - Automation
  - *What it is:* Build, test, deploy code automatically
  - *Why:* Integrated with GitHub, easy configuration

**Technology Selection Criteria:**
1. **Team Expertise:** Choose technologies your team knows
2. **Community Support:** Active community, good documentation
3. **Scalability:** Can it handle your growth?
4. **Cost:** Licensing, hosting, maintenance costs
5. **Integration:** Works well with other tools?
6. **Performance:** Meets your performance requirements?
7. **Security:** Built-in security features?
8. **Maintenance:** Easy to maintain and update?

**Deliverable:**
- Technology stack document with explanations
- Justification for each technology choice
- Comparison table if multiple options considered
- Reference to Technology_Stack_Glossary.md for details

---

### Step 6: Design Data Architecture
**What to do:**
- Identify entities and their relationships
- Choose database types (Relational, NoSQL, Graph, Time-series)
- Design data partitioning and sharding strategy
- Plan data backup and recovery
- Define data retention policies

**Deliverable:**
- High-level ER diagram
- Database selection rationale
- Data flow diagrams

---

### Step 7: Define APIs and Interfaces
**What to do:**
- List all APIs (internal and external)
- Define API contracts (request/response formats)
- Choose API style (REST, GraphQL, gRPC)
- Plan API versioning strategy
- Define authentication mechanism (OAuth, JWT, API Keys)

**Deliverable:**
- API catalog
- API contract specifications (OpenAPI/Swagger)

---

### Step 8: Address Non-Functional Requirements

#### 8.1 Scalability
- Horizontal vs Vertical scaling strategy
- Load balancing approach
- Auto-scaling policies

#### 8.2 Performance
- Expected response times
- Throughput requirements
- Caching strategy

#### 8.3 Security
- Authentication & Authorization
- Data encryption (at rest and in transit)
- Security compliance (GDPR, HIPAA, etc.)
- Vulnerability management

#### 8.4 Availability & Reliability
- Uptime requirements (99.9%, 99.99%)
- Disaster recovery plan
- Failover mechanisms
- Redundancy strategy

#### 8.5 Monitoring & Observability
- Logging strategy
- Metrics and monitoring
- Alerting mechanisms
- Distributed tracing

**Deliverable:**
- Non-functional requirements document
- SLA definitions

---

### Step 9: Identify Risks and Mitigation
**What to do:**
- List technical risks
- List business risks
- Define mitigation strategies
- Plan contingency approaches

**Deliverable:**
- Risk assessment matrix
- Mitigation plan

---

### Step 10: Create HLD Document
**What to do:**
- Compile all information into a comprehensive document
- Include all diagrams and specifications
- Get review from stakeholders
- Iterate based on feedback

**Deliverable:**
- Complete HLD document

---

## HLD Components

### 1. System Context Diagram
Shows the system and its external dependencies (users, external systems, third-party services)

### 2. Architecture Diagram
Shows major components and their interactions

### 3. Component Diagram
Details each component's purpose and interfaces

### 4. Deployment Diagram
Shows how components are deployed across infrastructure

### 5. Data Flow Diagram
Shows how data moves through the system

### 6. Sequence Diagram
Shows interaction flow for critical use cases

---

## Best Practices

1. **Keep it Simple:** Avoid unnecessary complexity
2. **Use Standard Notations:** UML, C4 Model, ArchiMate
3. **Be Technology Agnostic (when possible):** Focus on concepts, not specific tools
4. **Document Decisions:** Explain WHY certain choices were made
5. **Version Control:** Keep HLD in version control (Git)
6. **Regular Updates:** Update HLD as system evolves
7. **Stakeholder Review:** Get feedback from all stakeholders
8. **Traceability:** Link HLD to requirements
9. **Use Diagrams:** A picture is worth a thousand words
10. **Consider Future:** Design for extensibility and maintainability

---

## Example Template

See `HLD_Template_Example.md` for a complete example template you can use for your projects.

---

## Tools for Creating HLD

- **Diagramming:** Draw.io, Lucidchart, PlantUML, Mermaid, Microsoft Visio
- **Documentation:** Markdown, Confluence, Google Docs, Notion
- **Architecture:** ArchiMate, C4 Model
- **Collaboration:** Miro, Figma, Whimsical

---

## Next Steps

After completing HLD:
1. Get approval from stakeholders
2. Move to Low-Level Design (LLD) - see `LLD_Guide_Step_by_Step.md`
3. Start implementation based on approved designs
4. Keep HLD updated as system evolves

---

## Additional HLD Topics

### Step 11: Caching Strategy

**What to do:**
- Identify what data to cache
- Choose caching layers (Client, CDN, Application, Database)
- Select cache eviction policies (LRU, LFU, FIFO)
- Define cache invalidation strategy
- Plan cache warming

**Example:**
```
Caching Layers:
1. Browser Cache: Static assets (CSS, JS, images) - 7 days
2. CDN Cache: Product images, videos - 30 days
3. Application Cache (Redis):
   - User sessions - 24 hours
   - Product catalog - 1 hour
   - Search results - 15 minutes
4. Database Query Cache: Frequently accessed queries - 5 minutes

Cache Eviction: LRU (Least Recently Used)
Cache Size: 10GB Redis cluster
Hit Ratio Target: >80%
```

**Deliverable:**
- Caching architecture diagram
- Cache configuration document

---

### Step 12: Message Queue and Async Processing

**What to do:**
- Identify async operations (emails, notifications, reports)
- Choose message broker (Kafka, RabbitMQ, SQS)
- Design queue topology (topics, exchanges, queues)
- Plan retry and dead letter queues
- Define message formats

**Example:**
```
Message Broker: Apache Kafka

Topics:
1. order.created
   - Consumers: Inventory Service, Notification Service, Analytics
   - Retention: 7 days
   - Partitions: 10

2. payment.processed
   - Consumers: Order Service, Accounting Service
   - Retention: 30 days
   - Partitions: 5

3. email.send
   - Consumers: Email Service
   - Retention: 3 days
   - Partitions: 3

Dead Letter Queue: failed.messages
Retry Policy: Exponential backoff (3 attempts)
```

**Deliverable:**
- Message flow diagram
- Queue configuration

---

### Step 13: Search and Indexing

**What to do:**
- Identify search requirements (full-text, faceted, geo)
- Choose search engine (Elasticsearch, Solr, Algolia)
- Design index structure
- Plan search relevance tuning
- Define search performance targets

**Example:**
```
Search Engine: Elasticsearch

Indexes:
1. products_index
   - Fields: title, description, category, price, brand
   - Analyzers: Standard, Edge N-gram for autocomplete
   - Shards: 5, Replicas: 2

2. users_index
   - Fields: name, email, location
   - Shards: 2, Replicas: 1

Search Features:
- Autocomplete (as-you-type)
- Fuzzy matching (typo tolerance)
- Faceted search (filters by category, price, brand)
- Geo-search (location-based)

Performance Targets:
- Search response time: <100ms
- Index update latency: <5 seconds
```

**Deliverable:**
- Search architecture diagram
- Index schema

---

### Step 14: File Storage and CDN

**What to do:**
- Identify file types (images, videos, documents)
- Choose storage solution (S3, Azure Blob, GCS)
- Design folder structure and naming convention
- Plan CDN integration
- Define access control

**Example:**
```
Storage: AWS S3

Bucket Structure:
- prod-user-uploads/
  - avatars/{userId}/{timestamp}.jpg
  - documents/{userId}/{docId}.pdf

- prod-product-media/
  - images/{productId}/{size}/{imageId}.jpg
    - sizes: thumbnail, medium, large, original
  - videos/{productId}/{videoId}.mp4

CDN: CloudFront
- Edge locations: Global
- Cache behavior:
  - Images: 30 days
  - Videos: 90 days
- Signed URLs for private content

Access Control:
- Public: Product images
- Private: User documents (pre-signed URLs)
- Restricted: Admin uploads (IAM roles)

File Size Limits:
- Images: 10MB
- Videos: 500MB
- Documents: 50MB
```

**Deliverable:**
- Storage architecture diagram
- CDN configuration

---

### Step 15: Authentication and Authorization

**What to do:**
- Choose authentication method (JWT, OAuth, SAML)
- Design authorization model (RBAC, ABAC)
- Plan session management
- Define password policies
- Plan MFA if needed

**Example:**
```
Authentication: JWT (JSON Web Tokens)
- Access Token: 15 minutes expiry
- Refresh Token: 7 days expiry
- Token Storage: HTTP-only cookies

Authorization: RBAC (Role-Based Access Control)

Roles:
1. Admin
   - Permissions: ALL

2. Manager
   - Permissions:
     - products.create, products.update, products.delete
     - orders.view, orders.update
     - users.view

3. Customer
   - Permissions:
     - products.view
     - orders.create, orders.view (own)
     - profile.update (own)

4. Guest
   - Permissions:
     - products.view

Password Policy:
- Minimum 8 characters
- Must contain: uppercase, lowercase, number, special char
- Password history: Last 5 passwords
- Expiry: 90 days (for admin)
- Max failed attempts: 5 (account lockout)

MFA: Optional for users, Mandatory for admins
- Methods: TOTP (Google Authenticator), SMS
```

**Deliverable:**
- Authentication flow diagram
- Authorization matrix

---

### Step 16: Rate Limiting and Throttling

**What to do:**
- Identify rate limit requirements
- Choose rate limiting strategy (Fixed window, Sliding window, Token bucket)
- Define limits per user type
- Plan rate limit response handling
- Choose implementation (API Gateway, Redis)

**Example:**
```
Rate Limiting Strategy: Token Bucket Algorithm
Implementation: Redis + API Gateway

Limits by User Type:
1. Anonymous Users:
   - 100 requests per hour
   - 10 requests per minute

2. Authenticated Users:
   - 1000 requests per hour
   - 50 requests per minute

3. Premium Users:
   - 10,000 requests per hour
   - 200 requests per minute

4. API Partners:
   - Custom limits per contract
   - Burst allowance: 2x normal rate for 1 minute

Rate Limit Headers:
- X-RateLimit-Limit: 1000
- X-RateLimit-Remaining: 950
- X-RateLimit-Reset: 1678901234

Response when exceeded:
- Status: 429 Too Many Requests
- Retry-After: 3600 (seconds)
```

**Deliverable:**
- Rate limiting configuration
- Implementation approach

---

### Step 17: Disaster Recovery and Business Continuity

**What to do:**
- Define RTO (Recovery Time Objective)
- Define RPO (Recovery Point Objective)
- Plan backup strategy
- Design failover mechanism
- Plan disaster recovery testing

**Example:**
```
RTO: 4 hours (system must be operational within 4 hours)
RPO: 1 hour (maximum 1 hour of data loss acceptable)

Backup Strategy:
1. Database Backups:
   - Full backup: Daily at 2 AM UTC
   - Incremental backup: Every 6 hours
   - Retention: 30 days
   - Storage: S3 with cross-region replication

2. Application Backups:
   - Docker images: Versioned in ECR
   - Configuration: Git repository
   - Secrets: AWS Secrets Manager with backup

Failover Strategy:
- Primary Region: us-east-1
- Secondary Region: us-west-2
- Database: Multi-AZ with read replicas
- DNS: Route53 with health checks
- Failover time: <5 minutes (automated)

Disaster Recovery Testing:
- Frequency: Quarterly
- Scope: Full system failover
- Documentation: Runbook maintained
```

**Deliverable:**
- DR plan document
- Backup and restore procedures

---

### Step 18: Observability (Logging, Monitoring, Tracing)

**What to do:**
- Design logging strategy
- Plan metrics and monitoring
- Implement distributed tracing
- Define alerting rules
- Choose observability tools

**Example:**
```
Logging:
- Tool: ELK Stack (Elasticsearch, Logstash, Kibana)
- Log Levels: DEBUG, INFO, WARN, ERROR, FATAL
- Log Format: JSON structured logs
- Retention: 30 days
- Log aggregation: Centralized

Sample Log Entry:
{
  "timestamp": "2026-03-07T10:30:00Z",
  "level": "INFO",
  "service": "order-service",
  "traceId": "abc123",
  "userId": "user-456",
  "action": "order.created",
  "orderId": "order-789",
  "duration": 245,
  "message": "Order created successfully"
}

Monitoring:
- Tool: Prometheus + Grafana
- Metrics:
  - System: CPU, Memory, Disk, Network
  - Application: Request rate, Error rate, Duration (RED metrics)
  - Business: Orders/hour, Revenue, Active users
- Dashboards: Per service + Overall system health

Distributed Tracing:
- Tool: Jaeger
- Trace all requests across microservices
- Correlation ID in all logs
- Performance bottleneck identification

Alerting:
- Tool: PagerDuty + Slack
- Alert Rules:
  - Error rate > 5% for 5 minutes → Page on-call
  - Response time p95 > 1s for 10 minutes → Slack alert
  - CPU > 80% for 15 minutes → Slack alert
  - Database connections > 90% → Page on-call
  - Disk space < 10% → Page on-call
```

**Deliverable:**
- Observability architecture diagram
- Alerting runbook

---

### Step 19: API Gateway and Service Mesh

**What to do:**
- Choose API Gateway (Kong, AWS API Gateway, Apigee)
- Design routing rules
- Plan service mesh if using microservices (Istio, Linkerd)
- Define cross-cutting concerns (auth, logging, rate limiting)

**Example:**
```
API Gateway: Kong

Features:
1. Routing:
   - /api/v1/auth/* → Auth Service
   - /api/v1/products/* → Product Service
   - /api/v1/orders/* → Order Service

2. Authentication:
   - JWT validation
   - API key validation
   - OAuth 2.0 integration

3. Rate Limiting:
   - Per user/API key
   - Per endpoint

4. Request/Response Transformation:
   - Header injection (correlation ID)
   - Response formatting

5. Caching:
   - GET requests cached for 5 minutes

Service Mesh: Istio (for microservices)

Features:
1. Traffic Management:
   - Load balancing
   - Circuit breaking
   - Retry logic
   - Timeout configuration

2. Security:
   - mTLS between services
   - Service-to-service authentication

3. Observability:
   - Automatic metrics collection
   - Distributed tracing
   - Service dependency graph

Circuit Breaker Configuration:
- Failure threshold: 50%
- Timeout: 30 seconds
- Sleep window: 60 seconds
```

**Deliverable:**
- API Gateway configuration
- Service mesh architecture

---

### Step 20: Data Migration and ETL

**What to do:**
- Plan data migration strategy (if applicable)
- Design ETL pipelines
- Define data transformation rules
- Plan data validation
- Schedule batch jobs

**Example:**
```
Data Migration: Legacy System → New System

Strategy: Phased migration
1. Phase 1: Read-only data (products, categories)
2. Phase 2: User data
3. Phase 3: Transactional data (orders)
4. Phase 4: Switch over

ETL Pipeline:
- Tool: Apache Airflow

Jobs:
1. Daily Product Sync:
   - Extract: Legacy DB (MySQL)
   - Transform: Normalize data, enrich with metadata
   - Load: New DB (PostgreSQL) + Search Index (Elasticsearch)
   - Schedule: Daily at 3 AM
   - Duration: ~2 hours

2. Real-time Order Sync:
   - Extract: Kafka topic (order.created)
   - Transform: Calculate metrics, aggregate data
   - Load: Data warehouse (Snowflake)
   - Schedule: Continuous streaming

Data Validation:
- Row count verification
- Data integrity checks
- Reconciliation reports
- Rollback plan if validation fails

Batch Jobs:
1. Daily Reports: 4 AM
2. Weekly Analytics: Sunday 2 AM
3. Monthly Invoicing: 1st of month, 1 AM
```

**Deliverable:**
- Migration plan
- ETL pipeline diagram

---

### Step 21: Third-Party Integrations

**What to do:**
- Identify external services (payment, email, SMS, analytics)
- Design integration patterns (REST, Webhooks, SDK)
- Plan error handling for external failures
- Define SLA expectations
- Plan fallback mechanisms

**Example:**
```
Third-Party Integrations:

1. Payment Gateway: Stripe
   - Integration: REST API + Webhooks
   - Operations: Charge, Refund, Subscription
   - Webhooks: payment.succeeded, payment.failed
   - Retry: 3 attempts with exponential backoff
   - Fallback: Queue for manual processing
   - SLA: 99.9% uptime

2. Email Service: SendGrid
   - Integration: REST API
   - Operations: Send transactional emails
   - Rate limit: 100 emails/second
   - Retry: 5 attempts
   - Fallback: Secondary provider (AWS SES)
   - SLA: 99.5% uptime

3. SMS Service: Twilio
   - Integration: REST API
   - Operations: Send OTP, notifications
   - Rate limit: 50 SMS/second
   - Retry: 3 attempts
   - Fallback: Alternative provider
   - SLA: 99.95% uptime

4. Analytics: Google Analytics + Mixpanel
   - Integration: JavaScript SDK
   - Events: Page views, user actions, conversions
   - Batch: Send events in batches of 100
   - Retry: Client-side retry

5. Cloud Storage: AWS S3
   - Integration: AWS SDK
   - Operations: Upload, Download, Delete
   - Retry: Built-in SDK retry
   - Fallback: Local storage temporarily
   - SLA: 99.99% uptime

Integration Patterns:
- Circuit Breaker: Prevent cascading failures
- Timeout: 30 seconds max
- Idempotency: All operations idempotent
- Monitoring: Track success/failure rates
```

**Deliverable:**
- Integration architecture diagram
- Third-party service catalog

---

### Step 22: Compliance and Data Privacy

**What to do:**
- Identify compliance requirements (GDPR, HIPAA, PCI-DSS)
- Plan data privacy measures
- Design audit logging
- Plan data retention and deletion
- Define consent management

**Example:**
```
Compliance Requirements:

1. GDPR (General Data Protection Regulation):
   - Right to Access: API to export user data
   - Right to Erasure: Delete user data within 30 days
   - Right to Portability: Export data in JSON format
   - Consent Management: Explicit opt-in for marketing
   - Data Breach Notification: Within 72 hours
   - Privacy by Design: Encryption, pseudonymization

2. PCI-DSS (Payment Card Industry):
   - No storage of CVV
   - Tokenization of card numbers
   - Encrypted transmission (TLS 1.3)
   - Regular security audits
   - Access control to cardholder data

Data Privacy Measures:
- Encryption at rest: AES-256
- Encryption in transit: TLS 1.3
- PII masking in logs
- Data anonymization for analytics
- Secure key management (AWS KMS)

Audit Logging:
- Log all data access
- Log all data modifications
- Log all admin actions
- Retention: 7 years
- Immutable logs (WORM storage)

Data Retention:
- User data: Until account deletion + 30 days
- Transaction data: 7 years (legal requirement)
- Logs: 30 days (operational), 7 years (audit)
- Backups: 30 days

Consent Management:
- Marketing emails: Opt-in required
- Analytics cookies: Opt-in required
- Essential cookies: Implied consent
- Consent withdrawal: One-click unsubscribe
```

**Deliverable:**
- Compliance checklist
- Data privacy policy

---

### Step 23: Cost Optimization

**What to do:**
- Estimate infrastructure costs
- Identify cost optimization opportunities
- Plan resource right-sizing
- Define cost monitoring and alerts
- Plan reserved instances/savings plans

**Example:**
```
Cost Estimation (Monthly):

Infrastructure:
- Compute (EC2/EKS): $5,000
  - 10 instances × $0.50/hour × 730 hours
  - Optimization: Use spot instances for non-critical workloads (-60%)

- Database (RDS): $3,000
  - PostgreSQL db.r5.xlarge
  - Optimization: Use read replicas instead of larger instance

- Storage (S3): $500
  - 10TB × $0.023/GB
  - Optimization: Use S3 Intelligent-Tiering (-30%)

- CDN (CloudFront): $1,000
  - 50TB data transfer
  - Optimization: Optimize image sizes, use compression

- Cache (ElastiCache): $800
  - Redis cluster
  - Optimization: Right-size based on usage

- Message Queue (Kafka/MSK): $1,200
  - 3-node cluster

- Monitoring (CloudWatch): $300
  - Logs, metrics, alarms

Total Monthly: $11,800
Annual: ~$142,000

Cost Optimization Strategies:
1. Reserved Instances: Save 30-40% on compute
2. Auto-scaling: Scale down during off-peak hours
3. Spot Instances: Use for batch jobs (save 60-90%)
4. S3 Lifecycle Policies: Move old data to Glacier
5. Right-sizing: Monitor and adjust instance sizes
6. Delete unused resources: Regular audits
7. Use CDN caching: Reduce origin requests

Cost Monitoring:
- Tool: AWS Cost Explorer + CloudHealth
- Budgets: Set monthly budget alerts
- Alerts:
  - 80% of budget → Warning
  - 100% of budget → Critical alert
- Reports: Weekly cost breakdown by service
```

**Deliverable:**
- Cost estimation spreadsheet
- Cost optimization plan

---

### Step 24: Multi-tenancy (if applicable)

**What to do:**
- Choose tenancy model (Single-tenant, Multi-tenant, Hybrid)
- Design data isolation strategy
- Plan tenant provisioning
- Define resource allocation per tenant
- Plan tenant-specific customization

**Example:**
```
Tenancy Model: Multi-tenant with data isolation

Data Isolation Strategy: Schema per tenant

Database Design:
- Shared Database, Separate Schemas
  - tenant_1.users, tenant_1.orders
  - tenant_2.users, tenant_2.orders

Tenant Identification:
- Subdomain: tenant1.myapp.com
- Custom domain: customdomain.com (CNAME)
- Tenant ID in JWT token

Tenant Provisioning:
1. Signup → Create tenant record
2. Create database schema
3. Apply migrations
4. Create default admin user
5. Send welcome email
6. Provision time: <2 minutes

Resource Allocation:
- Free Tier:
  - 100 users
  - 1GB storage
  - 1000 API calls/day

- Pro Tier:
  - 1000 users
  - 10GB storage
  - 100,000 API calls/day

- Enterprise Tier:
  - Unlimited users
  - 1TB storage
  - Unlimited API calls
  - Dedicated resources

Tenant Customization:
- Branding: Logo, colors, domain
- Features: Enable/disable features per tenant
- Integrations: Tenant-specific API keys
- Workflows: Custom business rules
```

**Deliverable:**
- Multi-tenancy architecture diagram
- Tenant provisioning workflow

---

**Remember:** HLD is a living document. It should evolve with your system!

