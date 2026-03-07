# Technology Stack Glossary - Complete Reference

## 📚 Purpose
This document explains all common technologies used in system design with simple explanations, use cases, and when to choose them.

---

## 🎨 Frontend Technologies

### JavaScript Frameworks

#### **React.js**
- **What it is:** A JavaScript library for building user interfaces using reusable components
- **Created by:** Facebook (Meta)
- **Best for:** Single-page applications, dynamic UIs, mobile apps (React Native)
- **Key features:** Virtual DOM, component-based, large ecosystem
- **When to use:** Building interactive web applications
- **Example:** Facebook, Instagram, Netflix, Airbnb

#### **Angular**
- **What it is:** Full-featured TypeScript framework for building web applications
- **Created by:** Google
- **Best for:** Enterprise applications, complex forms, large teams
- **Key features:** Two-way data binding, dependency injection, CLI tools
- **When to use:** Large enterprise projects with strict structure
- **Example:** Google Cloud Console, Microsoft Office Online

#### **Vue.js**
- **What it is:** Progressive JavaScript framework for building UIs
- **Created by:** Evan You
- **Best for:** Small to medium projects, gradual adoption
- **Key features:** Easy learning curve, flexible, lightweight
- **When to use:** Projects needing simplicity and flexibility
- **Example:** Alibaba, GitLab, Xiaomi

### State Management

#### **Redux / Redux Toolkit**
- **What it is:** Predictable state container for JavaScript apps
- **Best for:** Complex state logic, multiple components sharing state
- **Key features:** Single source of truth, time-travel debugging
- **When to use:** Large apps with complex state management
- **Alternative:** MobX, Zustand, Recoil

#### **Context API**
- **What it is:** Built-in React feature for sharing state
- **Best for:** Simple state sharing, avoiding prop drilling
- **When to use:** Small to medium apps, theme/auth state
- **Alternative:** Redux for complex scenarios

### UI Component Libraries

#### **Material-UI (MUI)**
- **What it is:** React components implementing Google's Material Design
- **Best for:** Professional-looking apps quickly
- **Key features:** Pre-built components, theming, accessibility
- **When to use:** Need consistent, professional UI fast

#### **Ant Design**
- **What it is:** Enterprise-class UI design system
- **Best for:** Admin panels, dashboards, enterprise apps
- **When to use:** Building business applications

#### **Tailwind CSS**
- **What it is:** Utility-first CSS framework
- **Best for:** Custom designs, rapid prototyping
- **Key features:** No pre-built components, highly customizable
- **When to use:** Want full design control

### Build Tools

#### **Vite**
- **What it is:** Next-generation frontend build tool
- **Best for:** Modern web projects
- **Key features:** Lightning-fast HMR, optimized builds
- **When to use:** New projects, need fast development

#### **Webpack**
- **What it is:** Module bundler for JavaScript applications
- **Best for:** Complex build configurations
- **When to use:** Need fine-grained control over build process

---

## 🔧 Backend Technologies

### Programming Languages

#### **Node.js**
- **What it is:** JavaScript runtime built on Chrome's V8 engine
- **Best for:** Real-time apps, APIs, microservices
- **Key features:** Non-blocking I/O, event-driven, npm ecosystem
- **When to use:** I/O-heavy operations, real-time features
- **Example:** Netflix, LinkedIn, Uber

#### **Java**
- **What it is:** Object-oriented, platform-independent language
- **Best for:** Enterprise applications, Android apps
- **Key features:** Strong typing, JVM, mature ecosystem
- **When to use:** Large-scale enterprise systems
- **Example:** Amazon, eBay, LinkedIn backend

#### **Python**
- **What it is:** High-level, interpreted programming language
- **Best for:** Data science, ML, scripting, web apps
- **Key features:** Easy syntax, rich libraries, versatile
- **When to use:** Data processing, ML, rapid development
- **Example:** Instagram, Spotify, Dropbox

#### **Go (Golang)**
- **What it is:** Compiled language designed by Google
- **Best for:** Microservices, cloud-native apps, CLI tools
- **Key features:** Fast compilation, concurrency, simple syntax
- **When to use:** High-performance services, DevOps tools
- **Example:** Docker, Kubernetes, Uber

#### **C# (.NET)**
- **What it is:** Microsoft's object-oriented language
- **Best for:** Windows apps, enterprise software, games (Unity)
- **Key features:** Strong typing, .NET ecosystem, cross-platform
- **When to use:** Microsoft stack, enterprise applications
- **Example:** Stack Overflow, Bing, Xbox services

### Backend Frameworks

#### **Express.js (Node.js)**
- **What it is:** Minimal and flexible Node.js web framework
- **Best for:** RESTful APIs, web applications
- **Key features:** Middleware support, routing, minimal
- **When to use:** Building APIs quickly with Node.js

#### **Spring Boot (Java)**
- **What it is:** Production-ready Java framework
- **Best for:** Enterprise applications, microservices
- **Key features:** Auto-configuration, embedded servers, security
- **When to use:** Java-based enterprise systems

#### **FastAPI (Python)**
- **What it is:** Modern, fast Python web framework
- **Best for:** APIs with automatic documentation
- **Key features:** Async support, auto docs, type hints
- **When to use:** Building high-performance Python APIs

#### **Django (Python)**
- **What it is:** High-level Python web framework
- **Best for:** Full-stack web applications
- **Key features:** ORM, admin panel, batteries-included
- **When to use:** Rapid development of complete web apps

#### **Flask (Python)**
- **What it is:** Lightweight Python web framework
- **Best for:** Small to medium web apps, APIs
- **Key features:** Minimal, flexible, easy to learn
- **When to use:** Simple APIs, microservices

---

## 🗄️ Databases

### Relational Databases (SQL)

#### **PostgreSQL**
- **What it is:** Advanced open-source relational database
- **Best for:** Complex queries, data integrity, JSON data
- **Key features:** ACID compliance, extensions, JSON support
- **When to use:** Need strong data integrity, complex relationships
- **Example:** Instagram, Spotify, Reddit

#### **MySQL**
- **What it is:** Popular open-source relational database
- **Best for:** Web applications, read-heavy workloads
- **Key features:** Fast reads, replication, widely supported
- **When to use:** Traditional web apps, WordPress, e-commerce
- **Example:** Facebook, Twitter, YouTube

#### **Microsoft SQL Server**
- **What it is:** Enterprise relational database by Microsoft
- **Best for:** Windows environments, enterprise apps
- **Key features:** Integration with .NET, BI tools, high availability
- **When to use:** Microsoft stack, enterprise applications

### NoSQL Databases

#### **MongoDB**
- **What it is:** Document-oriented NoSQL database
- **Best for:** Flexible schemas, rapid development
- **Key features:** JSON-like documents, horizontal scaling, aggregation
- **When to use:** Flexible data models, rapid iteration
- **Example:** eBay, MetLife, Cisco

#### **Cassandra**
- **What it is:** Wide-column NoSQL database
- **Best for:** Time-series data, high write throughput
- **Key features:** Linear scalability, no single point of failure
- **When to use:** Massive scale, always-on applications
- **Example:** Netflix, Apple, Instagram

#### **DynamoDB**
- **What it is:** AWS-managed NoSQL database
- **Best for:** Serverless apps, key-value access
- **Key features:** Fully managed, auto-scaling, low latency
- **When to use:** AWS ecosystem, serverless architecture
- **Example:** Amazon.com, Lyft, Airbnb

#### **Redis**
- **What it is:** In-memory key-value data store
- **Best for:** Caching, session storage, real-time analytics
- **Key features:** Sub-millisecond latency, data structures, pub/sub
- **When to use:** Need ultra-fast data access
- **Example:** Twitter, GitHub, Stack Overflow

#### **Elasticsearch**
- **What it is:** Distributed search and analytics engine
- **Best for:** Full-text search, log analytics
- **Key features:** Real-time search, scalable, RESTful API
- **When to use:** Search functionality, log analysis
- **Example:** Wikipedia, GitHub, Netflix

### Graph Databases

#### **Neo4j**
- **What it is:** Graph database for connected data
- **Best for:** Social networks, recommendation engines
- **Key features:** Relationship-first, Cypher query language
- **When to use:** Complex relationships, network analysis
- **Example:** LinkedIn, eBay, Walmart

---

## ☁️ Cloud Providers

### **Amazon Web Services (AWS)**
- **What it is:** Leading cloud platform with 200+ services
- **Market share:** ~32% (largest)
- **Best for:** Startups to enterprises, comprehensive services
- **Key services:**
  - **EC2:** Virtual servers
  - **S3:** Object storage
  - **RDS:** Managed databases
  - **Lambda:** Serverless functions
  - **CloudFront:** CDN
  - **Route53:** DNS
- **When to use:** Need mature, comprehensive cloud platform
- **Example:** Netflix, Airbnb, NASA

### **Microsoft Azure**
- **What it is:** Microsoft's cloud computing platform
- **Market share:** ~23%
- **Best for:** Microsoft stack, enterprise, hybrid cloud
- **Key services:**
  - **Virtual Machines:** Compute
  - **Blob Storage:** Object storage
  - **Azure SQL:** Managed databases
  - **Functions:** Serverless
  - **CDN:** Content delivery
- **When to use:** Microsoft ecosystem, enterprise
- **Example:** BMW, Adobe, HP

### **Google Cloud Platform (GCP)**
- **What it is:** Google's cloud infrastructure
- **Market share:** ~10%
- **Best for:** Data analytics, ML, Kubernetes
- **Key services:**
  - **Compute Engine:** VMs
  - **Cloud Storage:** Object storage
  - **Cloud SQL:** Managed databases
  - **Cloud Functions:** Serverless
  - **BigQuery:** Data warehouse
- **When to use:** ML/AI, data analytics, Kubernetes
- **Example:** Spotify, Twitter, Snapchat

---

## 🔄 Message Queues & Event Streaming

### **Apache Kafka**
- **What it is:** Distributed event streaming platform
- **Best for:** Real-time data pipelines, event sourcing
- **Key features:** High throughput, fault-tolerant, message replay
- **When to use:** High-volume event streaming, microservices
- **Example:** LinkedIn, Uber, Netflix

### **RabbitMQ**
- **What it is:** Message broker implementing AMQP
- **Best for:** Task queues, request/reply patterns
- **Key features:** Flexible routing, multiple protocols
- **When to use:** Traditional message queuing
- **Example:** Reddit, 9GAG, Trivago

### **AWS SQS (Simple Queue Service)**
- **What it is:** Fully managed message queue service
- **Best for:** Decoupling microservices, AWS ecosystem
- **Key features:** Fully managed, scalable, reliable
- **When to use:** AWS-based architecture, simple queuing
- **Example:** Capital One, BMW, Expedia

### **Redis Pub/Sub**
- **What it is:** Publish/subscribe messaging in Redis
- **Best for:** Real-time notifications, chat apps
- **Key features:** Fast, simple, built into Redis
- **When to use:** Simple pub/sub, already using Redis
- **Example:** Chat applications, real-time dashboards

---

## 🌐 API & Networking

### **API Gateway**
- **What it is:** Entry point for all API requests
- **Purpose:** Route requests, authenticate, rate limit, transform
- **Options:**
  - **Kong:** Open-source, plugin-based
  - **AWS API Gateway:** Fully managed
  - **Apigee:** Enterprise-grade (Google)
  - **NGINX:** Lightweight, high-performance
- **When to use:** Microservices architecture, centralized API management

### **Load Balancer**
- **What it is:** Distributes traffic across multiple servers
- **Purpose:** High availability, fault tolerance, scalability
- **Types:**
  - **Application Load Balancer (Layer 7):** HTTP/HTTPS routing
  - **Network Load Balancer (Layer 4):** TCP/UDP traffic
  - **Classic Load Balancer:** Legacy, both layers
- **When to use:** Multiple servers, need high availability

### **CDN (Content Delivery Network)**
- **What it is:** Network of servers that cache content globally
- **Purpose:** Faster content delivery, reduced server load
- **Options:**
  - **CloudFront (AWS):** Integrated with AWS
  - **Cloudflare:** Free tier, DDoS protection
  - **Akamai:** Enterprise-grade
  - **Fastly:** Real-time purging
- **When to use:** Global users, static content, images/videos

### **DNS (Domain Name System)**
- **What it is:** Translates domain names to IP addresses
- **Purpose:** Route users to your servers
- **Options:**
  - **Route53 (AWS):** Health checks, routing policies
  - **Cloudflare DNS:** Fast, free, DDoS protection
  - **Google Cloud DNS:** Reliable, low-latency
- **When to use:** Need domain management, failover, geo-routing

---

## 🐳 Containers & Orchestration

### **Docker**
- **What it is:** Platform for containerizing applications
- **Purpose:** Package app with dependencies, consistent environments
- **Key features:** Lightweight, portable, version control
- **When to use:** Ensure consistency across dev/staging/prod
- **Example:** Every modern application

### **Kubernetes (K8s)**
- **What it is:** Container orchestration platform
- **Purpose:** Automate deployment, scaling, management of containers
- **Key features:** Auto-scaling, self-healing, rolling updates, service discovery
- **When to use:** Managing many containers, need auto-scaling
- **Managed options:** EKS (AWS), GKE (Google), AKS (Azure)

### **Docker Compose**
- **What it is:** Tool for defining multi-container Docker apps
- **Purpose:** Run multiple containers locally
- **When to use:** Local development, simple deployments
- **Not for:** Production (use Kubernetes instead)

---

## 🔍 Monitoring & Observability

### **Prometheus**
- **What it is:** Time-series database for metrics
- **Purpose:** Collect and store metrics (CPU, memory, requests)
- **Key features:** Pull-based, powerful queries (PromQL), alerting
- **When to use:** Need detailed metrics, open-source solution

### **Grafana**
- **What it is:** Visualization and analytics platform
- **Purpose:** Create dashboards from metrics
- **Key features:** Beautiful dashboards, multiple data sources, alerts
- **When to use:** Visualize Prometheus/other metrics

### **ELK Stack (Elasticsearch, Logstash, Kibana)**
- **Elasticsearch:** Store and search logs
- **Logstash:** Collect and transform logs
- **Kibana:** Visualize and search logs
- **Purpose:** Centralized logging and analysis
- **When to use:** Need to search/analyze logs from multiple services

### **Jaeger / Zipkin**
- **What it is:** Distributed tracing systems
- **Purpose:** Track requests across microservices
- **Key features:** Trace visualization, performance bottlenecks
- **When to use:** Microservices, need to debug slow requests

### **Datadog / New Relic**
- **What it is:** Commercial APM (Application Performance Monitoring)
- **Purpose:** All-in-one monitoring, logging, tracing
- **Key features:** Easy setup, beautiful dashboards, alerting
- **When to use:** Want managed solution, budget available

---

## 🔐 Security & Secrets

### **AWS Secrets Manager**
- **What it is:** Secure storage for secrets (passwords, API keys)
- **Purpose:** Centralized secret management, automatic rotation
- **When to use:** AWS ecosystem, need secret rotation

### **HashiCorp Vault**
- **What it is:** Open-source secrets management
- **Purpose:** Store and access secrets securely
- **Key features:** Dynamic secrets, encryption as a service
- **When to use:** Multi-cloud, need advanced features

### **Environment Variables**
- **What it is:** Configuration stored outside code
- **Purpose:** Different configs for dev/staging/prod
- **When to use:** Simple configuration, 12-factor apps

---

## 🚀 CI/CD Tools

### **GitHub Actions**
- **What it is:** Automation platform integrated with GitHub
- **Purpose:** Build, test, deploy code automatically
- **When to use:** Code on GitHub, need simple CI/CD

### **Jenkins**
- **What it is:** Open-source automation server
- **Purpose:** Build, test, deploy with plugins
- **When to use:** Need self-hosted, complex pipelines

### **GitLab CI/CD**
- **What it is:** Built-in CI/CD in GitLab
- **Purpose:** Integrated DevOps platform
- **When to use:** Using GitLab for code hosting

### **CircleCI / Travis CI**
- **What it is:** Cloud-based CI/CD platforms
- **Purpose:** Automated testing and deployment
- **When to use:** Need managed CI/CD solution

---

## 📊 Quick Decision Guide

### Choose Database:
- **Need ACID transactions?** → PostgreSQL, MySQL
- **Flexible schema?** → MongoDB
- **Ultra-fast reads?** → Redis
- **Full-text search?** → Elasticsearch
- **Time-series data?** → InfluxDB, TimescaleDB
- **Graph relationships?** → Neo4j

### Choose Cloud:
- **Most services, mature?** → AWS
- **Microsoft stack?** → Azure
- **ML/AI, Kubernetes?** → GCP
- **Budget-friendly?** → DigitalOcean, Linode

### Choose Message Queue:
- **High throughput, event streaming?** → Kafka
- **Simple queuing?** → RabbitMQ, SQS
- **Real-time pub/sub?** → Redis Pub/Sub

### Choose Frontend:
- **Large app, enterprise?** → Angular
- **Flexible, popular?** → React
- **Simple, easy learning?** → Vue
- **SEO important?** → Next.js (React), Nuxt.js (Vue)

---

**This glossary covers 90% of technologies you'll encounter in system design!**

