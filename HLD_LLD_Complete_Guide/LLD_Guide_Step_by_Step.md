# Low-Level Design (LLD) - Complete Step-by-Step Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Purpose of LLD](#purpose-of-lld)
3. [Step-by-Step LLD Process](#step-by-step-lld-process)
4. [LLD Components](#lld-components)
5. [Best Practices](#best-practices)
6. [Example Template](#example-template)

---

## Introduction

Low-Level Design (LLD) is a detailed design document that describes the internal workings of each component identified in the High-Level Design (HLD). It focuses on implementation details, algorithms, data structures, class diagrams, and module specifications.

**Target Audience:** Developers, QA Engineers, Technical Leads

---

## Purpose of LLD

- **Implementation Blueprint:** Provide detailed specifications for developers
- **Class & Module Design:** Define classes, methods, and their interactions
- **Algorithm Design:** Specify algorithms and logic flow
- **Database Schema:** Detailed table structures, indexes, relationships
- **Code-Level Details:** Interface definitions, data structures, error handling
- **Testing Guide:** Provide basis for unit and integration testing

---

## Step-by-Step LLD Process

### Step 1: Review HLD
**What to do:**
- Thoroughly understand the High-Level Design
- Identify the component you're designing in detail
- Understand component responsibilities and interfaces
- Review non-functional requirements

**Deliverable:**
- Component understanding document
- Questions/clarifications list

---

### Step 2: Define Module Structure
**What to do:**
- Break down component into modules/packages
- Define module responsibilities
- Identify dependencies between modules
- Plan folder/package structure

**Example Structure:**
```
/src
  /controllers    # Handle HTTP requests
  /services       # Business logic
  /repositories   # Data access layer
  /models         # Data models/entities
  /utils          # Utility functions
  /middleware     # Request interceptors
  /validators     # Input validation
  /config         # Configuration
```

**Deliverable:**
- Module structure diagram
- Package/folder hierarchy

---

### Step 3: Design Classes and Objects
**What to do:**
- Identify classes/objects for each module
- Define class attributes (properties/fields)
- Define class methods (functions/operations)
- Define relationships (inheritance, composition, aggregation)
- Apply SOLID principles
- Apply design patterns where appropriate

**Deliverable:**
- Class diagrams (UML)
- Class specifications document

---

### Step 4: Define Data Models
**What to do:**
- Design detailed database schema
- Define tables with all columns and data types
- Define primary keys, foreign keys, indexes
- Define constraints (NOT NULL, UNIQUE, CHECK)
- Plan database migrations
- Design for normalization (or denormalization if needed)

**Deliverable:**
- Detailed ER diagram
- Database schema scripts (DDL)
- Migration scripts

---

### Step 5: Design APIs in Detail
**What to do:**
- Define each API endpoint
- Specify request parameters, headers, body
- Specify response format, status codes
- Define error responses
- Document authentication/authorization requirements
- Add request/response examples

**Example:**
```
POST /api/v1/users
Request:
{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "admin"
}

Response (201 Created):
{
  "id": "uuid-123",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "admin",
  "createdAt": "2026-03-07T10:00:00Z"
}

Error Response (400 Bad Request):
{
  "error": "VALIDATION_ERROR",
  "message": "Email already exists",
  "field": "email"
}
```

**Deliverable:**
- Detailed API specification (OpenAPI/Swagger)
- API documentation

---

### Step 6: Design Algorithms and Logic
**What to do:**
- Define algorithms for complex operations
- Create flowcharts for business logic
- Specify pseudocode for critical functions
- Define state machines if applicable
- Plan error handling and edge cases

**Deliverable:**
- Flowcharts
- Pseudocode
- Algorithm complexity analysis (Big O notation)

---

### Step 7: Define Data Structures
**What to do:**
- Choose appropriate data structures (Arrays, Lists, Maps, Sets, Trees, Graphs)
- Define custom data structures if needed
- Specify data transformation logic
- Plan data validation rules

**Deliverable:**
- Data structure specifications
- Validation rules document

---

### Step 8: Design Error Handling
**What to do:**
- Define error types and error codes
- Design exception hierarchy
- Plan error logging strategy
- Define user-facing error messages
- Plan retry mechanisms for failures

**Example Error Hierarchy:**
```
ApplicationError
├── ValidationError
│   ├── InvalidEmailError
│   └── InvalidPasswordError
├── AuthenticationError
│   ├── InvalidCredentialsError
│   └── TokenExpiredError
├── AuthorizationError
├── NotFoundError
└── InternalServerError
```

**Deliverable:**
- Error handling specification
- Error code catalog

---

### Step 9: Design Security Mechanisms
**What to do:**
- Define authentication flow (login, logout, token refresh)
- Define authorization rules (RBAC, ABAC)
- Plan input validation and sanitization
- Design encryption mechanisms
- Plan security headers and CORS policies
- Define rate limiting rules

**Deliverable:**
- Security implementation guide
- Authentication/Authorization flow diagrams

---

### Step 10: Define Interfaces and Contracts
**What to do:**
- Define interfaces for each module
- Specify method signatures
- Define input/output contracts
- Plan dependency injection
- Define abstract classes/interfaces

**Example (TypeScript):**
```typescript
interface IUserRepository {
  findById(id: string): Promise<User | null>;
  findByEmail(email: string): Promise<User | null>;
  create(user: CreateUserDTO): Promise<User>;
  update(id: string, user: UpdateUserDTO): Promise<User>;
  delete(id: string): Promise<void>;
}
```

**Deliverable:**
- Interface definitions
- Contract specifications

---

### Step 11: Design Testing Strategy
**What to do:**
- Plan unit tests for each class/method
- Define integration test scenarios
- Plan test data and fixtures
- Define mocking strategy
- Specify code coverage targets

**Deliverable:**
- Test plan document
- Test case specifications

---

### Step 12: Design Logging and Monitoring
**What to do:**
- Define log levels (DEBUG, INFO, WARN, ERROR)
- Specify what to log at each level
- Plan structured logging format
- Define metrics to track
- Plan correlation IDs for distributed tracing

**Deliverable:**
- Logging specification
- Monitoring metrics list

---

### Step 13: Design Configuration Management
**What to do:**
- Define configuration parameters
- Plan environment-specific configs (dev, staging, prod)
- Design secrets management
- Plan feature flags if needed

**Deliverable:**
- Configuration schema
- Environment configuration templates

---

### Step 14: Create Detailed Sequence Diagrams
**What to do:**
- Create sequence diagrams for each use case
- Show object interactions
- Include method calls and returns
- Show conditional flows and loops

**Deliverable:**
- Sequence diagrams for all major flows

---

### Step 15: Document Code Standards
**What to do:**
- Define naming conventions
- Define code formatting rules
- Specify commenting standards
- Define file organization rules
- Plan code review checklist

**Deliverable:**
- Coding standards document
- Code review guidelines

---

### Step 16: Create LLD Document
**What to do:**
- Compile all specifications into comprehensive document
- Include all diagrams and code examples
- Get review from team
- Iterate based on feedback

**Deliverable:**
- Complete LLD document

---

## LLD Components

### 1. Class Diagrams
Shows classes, attributes, methods, and relationships

### 2. Sequence Diagrams
Shows object interactions over time

### 3. Activity Diagrams
Shows workflow and business logic flow

### 4. State Diagrams
Shows state transitions for stateful objects

### 5. Database Schema
Detailed table structures with all constraints

### 6. API Specifications
Complete API documentation with examples

### 7. Pseudocode/Algorithms
Detailed logic for complex operations

---

## Best Practices

1. **Be Specific:** Provide enough detail for developers to implement
2. **Use Standard Notations:** UML, ERD standards
3. **Follow Design Principles:** SOLID, DRY, KISS, YAGNI
4. **Apply Design Patterns:** Factory, Singleton, Strategy, Observer, etc.
5. **Think About Edge Cases:** Handle errors, nulls, empty data
6. **Consider Performance:** Algorithm complexity, database indexes
7. **Plan for Testing:** Design testable code
8. **Document Assumptions:** Make implicit knowledge explicit
9. **Keep It Updated:** Update LLD as implementation evolves
10. **Code Examples:** Include code snippets where helpful

---

## Design Patterns to Consider

- **Creational:** Singleton, Factory, Builder, Prototype
- **Structural:** Adapter, Decorator, Facade, Proxy
- **Behavioral:** Strategy, Observer, Command, State, Template Method

---

## SOLID Principles

- **S**ingle Responsibility Principle
- **O**pen/Closed Principle
- **L**iskov Substitution Principle
- **I**nterface Segregation Principle
- **D**ependency Inversion Principle

---

## Example Template

See `LLD_Template_Example.md` for a complete example template you can use for your projects.

---

## Tools for Creating LLD

- **UML Diagrams:** PlantUML, StarUML, Visual Paradigm, Draw.io
- **Database Design:** dbdiagram.io, MySQL Workbench, pgAdmin
- **API Documentation:** Swagger/OpenAPI, Postman
- **Code Documentation:** JSDoc, Javadoc, Sphinx, Doxygen
- **Collaboration:** Confluence, Notion, Google Docs

---

## Next Steps

After completing LLD:
1. Get review from technical team
2. Start implementation following the LLD
3. Write unit tests based on LLD specifications
4. Update LLD if implementation reveals better approaches
5. Use LLD for code reviews

---

## Additional LLD Topics

### Step 17: Dependency Injection and IoC

**What to do:**
- Design dependency injection container
- Define service lifetimes (Singleton, Scoped, Transient)
- Plan interface-based programming
- Configure DI container

**Example (TypeScript with InversifyJS):**
```typescript
// Define interfaces
interface IUserRepository {
  findById(id: string): Promise<User | null>;
  create(user: User): Promise<User>;
}

interface IEmailService {
  sendEmail(to: string, subject: string, body: string): Promise<void>;
}

interface IUserService {
  registerUser(data: RegisterDTO): Promise<User>;
}

// Implement classes
@injectable()
class UserRepository implements IUserRepository {
  constructor(@inject('Database') private db: Database) {}

  async findById(id: string): Promise<User | null> {
    return this.db.query('SELECT * FROM users WHERE id = $1', [id]);
  }

  async create(user: User): Promise<User> {
    return this.db.query('INSERT INTO users ...', [user]);
  }
}

@injectable()
class EmailService implements IEmailService {
  async sendEmail(to: string, subject: string, body: string): Promise<void> {
    // Implementation
  }
}

@injectable()
class UserService implements IUserService {
  constructor(
    @inject('IUserRepository') private userRepo: IUserRepository,
    @inject('IEmailService') private emailService: IEmailService
  ) {}

  async registerUser(data: RegisterDTO): Promise<User> {
    const user = await this.userRepo.create(data);
    await this.emailService.sendEmail(user.email, 'Welcome', 'Welcome!');
    return user;
  }
}

// DI Container Configuration
const container = new Container();
container.bind<IUserRepository>('IUserRepository').to(UserRepository).inSingletonScope();
container.bind<IEmailService>('IEmailService').to(EmailService).inSingletonScope();
container.bind<IUserService>('IUserService').to(UserService).inRequestScope();

// Service Lifetimes:
// - Singleton: One instance for entire application
// - Scoped: One instance per request
// - Transient: New instance every time
```

**Deliverable:**
- DI container configuration
- Interface definitions
- Lifetime specifications

---

### Step 18: Caching Implementation

**What to do:**
- Design cache layer
- Implement cache-aside pattern
- Define cache keys and TTL
- Plan cache invalidation
- Handle cache stampede

**Example:**
```typescript
interface ICacheService {
  get<T>(key: string): Promise<T | null>;
  set<T>(key: string, value: T, ttl: number): Promise<void>;
  delete(key: string): Promise<void>;
  deletePattern(pattern: string): Promise<void>;
}

class RedisCacheService implements ICacheService {
  constructor(private redis: Redis) {}

  async get<T>(key: string): Promise<T | null> {
    const value = await this.redis.get(key);
    return value ? JSON.parse(value) : null;
  }

  async set<T>(key: string, value: T, ttl: number): Promise<void> {
    await this.redis.setex(key, ttl, JSON.stringify(value));
  }

  async delete(key: string): Promise<void> {
    await this.redis.del(key);
  }

  async deletePattern(pattern: string): Promise<void> {
    const keys = await this.redis.keys(pattern);
    if (keys.length > 0) {
      await this.redis.del(...keys);
    }
  }
}

// Cache-Aside Pattern Implementation
class ProductService {
  constructor(
    private productRepo: IProductRepository,
    private cache: ICacheService
  ) {}

  async getProduct(id: string): Promise<Product | null> {
    const cacheKey = `product:${id}`;

    // Try cache first
    let product = await this.cache.get<Product>(cacheKey);

    if (product) {
      return product; // Cache hit
    }

    // Cache miss - fetch from database
    product = await this.productRepo.findById(id);

    if (product) {
      // Store in cache for 1 hour
      await this.cache.set(cacheKey, product, 3600);
    }

    return product;
  }

  async updateProduct(id: string, data: UpdateProductDTO): Promise<Product> {
    const product = await this.productRepo.update(id, data);

    // Invalidate cache
    await this.cache.delete(`product:${id}`);
    await this.cache.deletePattern(`products:list:*`);

    return product;
  }
}

// Cache Stampede Prevention (using locks)
class ProductServiceWithLock {
  private locks = new Map<string, Promise<Product | null>>();

  async getProduct(id: string): Promise<Product | null> {
    const cacheKey = `product:${id}`;

    // Check cache
    let product = await this.cache.get<Product>(cacheKey);
    if (product) return product;

    // Check if another request is already fetching
    if (this.locks.has(cacheKey)) {
      return this.locks.get(cacheKey)!;
    }

    // Create lock and fetch
    const fetchPromise = this.fetchAndCache(id, cacheKey);
    this.locks.set(cacheKey, fetchPromise);

    try {
      product = await fetchPromise;
      return product;
    } finally {
      this.locks.delete(cacheKey);
    }
  }

  private async fetchAndCache(id: string, cacheKey: string): Promise<Product | null> {
    const product = await this.productRepo.findById(id);
    if (product) {
      await this.cache.set(cacheKey, product, 3600);
    }
    return product;
  }
}

// Cache Key Naming Convention:
// - product:{id} - Single product
// - products:list:{page}:{limit} - Product list
// - user:{id}:cart - User's cart
// - search:{query}:{filters} - Search results

// TTL Strategy:
// - Static data (categories): 24 hours
// - Semi-static (products): 1 hour
// - Dynamic (cart): 15 minutes
// - Real-time (inventory): 1 minute
```

**Deliverable:**
- Cache service implementation
- Cache key naming convention
- TTL configuration
- Invalidation strategy

---

### Step 19: Database Transaction Management

**What to do:**
- Design transaction boundaries
- Implement transaction handling
- Plan isolation levels
- Handle distributed transactions (if needed)
- Implement optimistic/pessimistic locking

**Example:**
```typescript
// Transaction Manager Interface
interface ITransactionManager {
  beginTransaction(): Promise<Transaction>;
  commit(transaction: Transaction): Promise<void>;
  rollback(transaction: Transaction): Promise<void>;
}

// Repository with Transaction Support
class OrderRepository {
  constructor(private db: Database) {}

  async createOrder(order: Order, transaction?: Transaction): Promise<Order> {
    const client = transaction?.client || this.db;
    const result = await client.query(
      'INSERT INTO orders (user_id, total, status) VALUES ($1, $2, $3) RETURNING *',
      [order.userId, order.total, order.status]
    );
    return result.rows[0];
  }

  async createOrderItems(orderId: string, items: OrderItem[], transaction?: Transaction): Promise<void> {
    const client = transaction?.client || this.db;
    for (const item of items) {
      await client.query(
        'INSERT INTO order_items (order_id, product_id, quantity, price) VALUES ($1, $2, $3, $4)',
        [orderId, item.productId, item.quantity, item.price]
      );
    }
  }

  async updateInventory(productId: string, quantity: number, transaction?: Transaction): Promise<void> {
    const client = transaction?.client || this.db;
    await client.query(
      'UPDATE products SET inventory = inventory - $1 WHERE id = $2',
      [quantity, productId]
    );
  }
}

// Service with Transaction Management
class OrderService {
  constructor(
    private orderRepo: OrderRepository,
    private transactionManager: ITransactionManager
  ) {}

  async createOrder(userId: string, items: CartItem[]): Promise<Order> {
    const transaction = await this.transactionManager.beginTransaction();

    try {
      // 1. Create order
      const order = await this.orderRepo.createOrder({
        userId,
        total: this.calculateTotal(items),
        status: 'pending'
      }, transaction);

      // 2. Create order items
      await this.orderRepo.createOrderItems(order.id, items, transaction);

      // 3. Update inventory
      for (const item of items) {
        await this.orderRepo.updateInventory(item.productId, item.quantity, transaction);
      }

      // 4. Commit transaction
      await this.transactionManager.commit(transaction);

      return order;
    } catch (error) {
      // Rollback on any error
      await this.transactionManager.rollback(transaction);
      throw error;
    }
  }
}

// Optimistic Locking (using version field)
class ProductRepository {
  async updateWithOptimisticLock(id: string, data: UpdateProductDTO, version: number): Promise<Product> {
    const result = await this.db.query(
      `UPDATE products
       SET name = $1, price = $2, version = version + 1, updated_at = NOW()
       WHERE id = $3 AND version = $4
       RETURNING *`,
      [data.name, data.price, id, version]
    );

    if (result.rows.length === 0) {
      throw new ConcurrentModificationError('Product was modified by another user');
    }

    return result.rows[0];
  }
}

// Pessimistic Locking (using SELECT FOR UPDATE)
class InventoryRepository {
  async reserveInventory(productId: string, quantity: number, transaction: Transaction): Promise<void> {
    // Lock the row
    const result = await transaction.client.query(
      'SELECT inventory FROM products WHERE id = $1 FOR UPDATE',
      [productId]
    );

    const currentInventory = result.rows[0].inventory;

    if (currentInventory < quantity) {
      throw new InsufficientInventoryError('Not enough inventory');
    }

    // Update inventory
    await transaction.client.query(
      'UPDATE products SET inventory = inventory - $1 WHERE id = $2',
      [quantity, productId]
    );
  }
}

// Isolation Levels:
// - READ UNCOMMITTED: Dirty reads possible
// - READ COMMITTED: Default, prevents dirty reads
// - REPEATABLE READ: Prevents non-repeatable reads
// - SERIALIZABLE: Highest isolation, prevents phantom reads

// Set isolation level:
await client.query('SET TRANSACTION ISOLATION LEVEL SERIALIZABLE');
```

**Deliverable:**
- Transaction management implementation
- Locking strategy
- Isolation level configuration
- Error handling for transactions

---

### Step 20: Event-Driven Architecture Implementation

**What to do:**
- Design event schema
- Implement event publisher
- Implement event subscribers
- Plan event versioning
- Handle event ordering and idempotency

**Example:**
```typescript
// Event Base Class
abstract class DomainEvent {
  public readonly eventId: string;
  public readonly eventType: string;
  public readonly timestamp: Date;
  public readonly version: number;

  constructor(eventType: string, version: number = 1) {
    this.eventId = uuidv4();
    this.eventType = eventType;
    this.timestamp = new Date();
    this.version = version;
  }

  abstract toJSON(): object;
}

// Specific Events
class OrderCreatedEvent extends DomainEvent {
  constructor(
    public readonly orderId: string,
    public readonly userId: string,
    public readonly total: number,
    public readonly items: OrderItem[]
  ) {
    super('order.created', 1);
  }

  toJSON() {
    return {
      eventId: this.eventId,
      eventType: this.eventType,
      timestamp: this.timestamp,
      version: this.version,
      data: {
        orderId: this.orderId,
        userId: this.userId,
        total: this.total,
        items: this.items
      }
    };
  }
}

class PaymentProcessedEvent extends DomainEvent {
  constructor(
    public readonly paymentId: string,
    public readonly orderId: string,
    public readonly amount: number,
    public readonly status: 'success' | 'failed'
  ) {
    super('payment.processed', 1);
  }

  toJSON() {
    return {
      eventId: this.eventId,
      eventType: this.eventType,
      timestamp: this.timestamp,
      version: this.version,
      data: {
        paymentId: this.paymentId,
        orderId: this.orderId,
        amount: this.amount,
        status: this.status
      }
    };
  }
}

// Event Publisher Interface
interface IEventPublisher {
  publish(event: DomainEvent): Promise<void>;
  publishBatch(events: DomainEvent[]): Promise<void>;
}

// Kafka Event Publisher
class KafkaEventPublisher implements IEventPublisher {
  constructor(private kafka: Kafka) {}

  async publish(event: DomainEvent): Promise<void> {
    const producer = this.kafka.producer();
    await producer.connect();

    await producer.send({
      topic: event.eventType,
      messages: [{
        key: event.eventId,
        value: JSON.stringify(event.toJSON()),
        headers: {
          'event-type': event.eventType,
          'event-version': event.version.toString()
        }
      }]
    });

    await producer.disconnect();
  }

  async publishBatch(events: DomainEvent[]): Promise<void> {
    const producer = this.kafka.producer();
    await producer.connect();

    const messagesByTopic = new Map<string, any[]>();

    for (const event of events) {
      if (!messagesByTopic.has(event.eventType)) {
        messagesByTopic.set(event.eventType, []);
      }

      messagesByTopic.get(event.eventType)!.push({
        key: event.eventId,
        value: JSON.stringify(event.toJSON())
      });
    }

    for (const [topic, messages] of messagesByTopic) {
      await producer.send({ topic, messages });
    }

    await producer.disconnect();
  }
}

// Event Subscriber Interface
interface IEventSubscriber<T extends DomainEvent> {
  handle(event: T): Promise<void>;
}

// Specific Subscribers
class OrderCreatedSubscriber implements IEventSubscriber<OrderCreatedEvent> {
  constructor(
    private emailService: IEmailService,
    private inventoryService: IInventoryService
  ) {}

  async handle(event: OrderCreatedEvent): Promise<void> {
    // Send confirmation email
    await this.emailService.sendOrderConfirmation(event.userId, event.orderId);

    // Update inventory
    await this.inventoryService.reserveItems(event.items);

    // Log event
    logger.info('Order created event processed', {
      eventId: event.eventId,
      orderId: event.orderId
    });
  }
}

// Event Consumer (Kafka)
class KafkaEventConsumer {
  private subscribers = new Map<string, IEventSubscriber<any>[]>();

  constructor(private kafka: Kafka) {}

  subscribe<T extends DomainEvent>(eventType: string, subscriber: IEventSubscriber<T>): void {
    if (!this.subscribers.has(eventType)) {
      this.subscribers.set(eventType, []);
    }
    this.subscribers.get(eventType)!.push(subscriber);
  }

  async start(): Promise<void> {
    const consumer = this.kafka.consumer({ groupId: 'order-service' });
    await consumer.connect();

    const topics = Array.from(this.subscribers.keys());
    await consumer.subscribe({ topics });

    await consumer.run({
      eachMessage: async ({ topic, partition, message }) => {
        const event = JSON.parse(message.value!.toString());
        const subscribers = this.subscribers.get(topic) || [];

        // Process with idempotency check
        const eventId = event.eventId;
        if (await this.isProcessed(eventId)) {
          logger.info('Event already processed, skipping', { eventId });
          return;
        }

        // Process event
        for (const subscriber of subscribers) {
          try {
            await subscriber.handle(event);
          } catch (error) {
            logger.error('Error processing event', { eventId, error });
            // Send to dead letter queue
            await this.sendToDeadLetterQueue(event, error);
          }
        }

        // Mark as processed
        await this.markAsProcessed(eventId);
      }
    });
  }

  private async isProcessed(eventId: string): Promise<boolean> {
    // Check in database or cache
    const result = await db.query(
      'SELECT 1 FROM processed_events WHERE event_id = $1',
      [eventId]
    );
    return result.rows.length > 0;
  }

  private async markAsProcessed(eventId: string): Promise<void> {
    await db.query(
      'INSERT INTO processed_events (event_id, processed_at) VALUES ($1, NOW())',
      [eventId]
    );
  }

  private async sendToDeadLetterQueue(event: any, error: Error): Promise<void> {
    // Implementation
  }
}

// Usage in Service
class OrderService {
  constructor(
    private orderRepo: IOrderRepository,
    private eventPublisher: IEventPublisher
  ) {}

  async createOrder(userId: string, items: CartItem[]): Promise<Order> {
    // Create order
    const order = await this.orderRepo.create({
      userId,
      items,
      total: this.calculateTotal(items),
      status: 'pending'
    });

    // Publish event
    const event = new OrderCreatedEvent(
      order.id,
      order.userId,
      order.total,
      order.items
    );
    await this.eventPublisher.publish(event);

    return order;
  }
}
```

**Deliverable:**
- Event schema definitions
- Publisher/Subscriber implementations
- Idempotency handling
- Dead letter queue strategy

---

### Step 21: Pagination and Filtering Implementation

**What to do:**
- Design pagination strategy (Offset, Cursor-based)
- Implement filtering
- Implement sorting
- Plan performance optimization

**Example:**
```typescript
// Pagination DTOs
interface PaginationParams {
  page?: number;
  limit?: number;
  cursor?: string;
}

interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    total: number;
    page: number;
    limit: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

interface CursorPaginatedResponse<T> {
  data: T[];
  pagination: {
    nextCursor: string | null;
    previousCursor: string | null;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

// Filter and Sort DTOs
interface FilterParams {
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  brand?: string;
  inStock?: boolean;
  search?: string;
}

interface SortParams {
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

// Offset-based Pagination
class ProductRepository {
  async findAll(
    filters: FilterParams,
    sort: SortParams,
    pagination: PaginationParams
  ): Promise<PaginatedResponse<Product>> {
    const page = pagination.page || 1;
    const limit = pagination.limit || 20;
    const offset = (page - 1) * limit;

    // Build WHERE clause
    const whereClauses: string[] = [];
    const params: any[] = [];
    let paramIndex = 1;

    if (filters.category) {
      whereClauses.push(`category = $${paramIndex++}`);
      params.push(filters.category);
    }

    if (filters.minPrice !== undefined) {
      whereClauses.push(`price >= $${paramIndex++}`);
      params.push(filters.minPrice);
    }

    if (filters.maxPrice !== undefined) {
      whereClauses.push(`price <= $${paramIndex++}`);
      params.push(filters.maxPrice);
    }

    if (filters.brand) {
      whereClauses.push(`brand = $${paramIndex++}`);
      params.push(filters.brand);
    }

    if (filters.inStock !== undefined) {
      whereClauses.push(`inventory > 0`);
    }

    if (filters.search) {
      whereClauses.push(`(name ILIKE $${paramIndex} OR description ILIKE $${paramIndex})`);
      params.push(`%${filters.search}%`);
      paramIndex++;
    }

    const whereClause = whereClauses.length > 0
      ? `WHERE ${whereClauses.join(' AND ')}`
      : '';

    // Build ORDER BY clause
    const sortBy = sort.sortBy || 'created_at';
    const sortOrder = sort.sortOrder || 'desc';
    const orderByClause = `ORDER BY ${sortBy} ${sortOrder}`;

    // Count total
    const countQuery = `SELECT COUNT(*) FROM products ${whereClause}`;
    const countResult = await this.db.query(countQuery, params);
    const total = parseInt(countResult.rows[0].count);

    // Fetch data
    const dataQuery = `
      SELECT * FROM products
      ${whereClause}
      ${orderByClause}
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1}
    `;
    params.push(limit, offset);

    const dataResult = await this.db.query(dataQuery, params);

    return {
      data: dataResult.rows,
      pagination: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit),
        hasNext: page * limit < total,
        hasPrevious: page > 1
      }
    };
  }
}

// Cursor-based Pagination (better for real-time data)
class ProductRepositoryCursor {
  async findAll(
    filters: FilterParams,
    sort: SortParams,
    cursor?: string,
    limit: number = 20
  ): Promise<CursorPaginatedResponse<Product>> {
    const whereClauses: string[] = [];
    const params: any[] = [];
    let paramIndex = 1;

    // Apply filters (same as above)
    // ...

    // Decode cursor
    let cursorCondition = '';
    if (cursor) {
      const decodedCursor = this.decodeCursor(cursor);
      cursorCondition = `AND created_at < $${paramIndex++}`;
      params.push(decodedCursor.createdAt);
    }

    const whereClause = whereClauses.length > 0
      ? `WHERE ${whereClauses.join(' AND ')} ${cursorCondition}`
      : cursorCondition ? `WHERE ${cursorCondition.replace('AND ', '')}` : '';

    // Fetch limit + 1 to check if there's more
    const query = `
      SELECT * FROM products
      ${whereClause}
      ORDER BY created_at DESC
      LIMIT $${paramIndex}
    `;
    params.push(limit + 1);

    const result = await this.db.query(query, params);
    const hasNext = result.rows.length > limit;
    const data = hasNext ? result.rows.slice(0, -1) : result.rows;

    const nextCursor = hasNext
      ? this.encodeCursor({ createdAt: data[data.length - 1].created_at })
      : null;

    return {
      data,
      pagination: {
        nextCursor,
        previousCursor: null, // Implement if needed
        hasNext,
        hasPrevious: !!cursor
      }
    };
  }

  private encodeCursor(data: { createdAt: Date }): string {
    return Buffer.from(JSON.stringify(data)).toString('base64');
  }

  private decodeCursor(cursor: string): { createdAt: Date } {
    return JSON.parse(Buffer.from(cursor, 'base64').toString());
  }
}

// API Controller
class ProductController {
  async getProducts(req: Request, res: Response): Promise<void> {
    const filters: FilterParams = {
      category: req.query.category as string,
      minPrice: req.query.minPrice ? parseFloat(req.query.minPrice as string) : undefined,
      maxPrice: req.query.maxPrice ? parseFloat(req.query.maxPrice as string) : undefined,
      brand: req.query.brand as string,
      inStock: req.query.inStock === 'true',
      search: req.query.search as string
    };

    const sort: SortParams = {
      sortBy: req.query.sortBy as string || 'created_at',
      sortOrder: (req.query.sortOrder as 'asc' | 'desc') || 'desc'
    };

    const pagination: PaginationParams = {
      page: req.query.page ? parseInt(req.query.page as string) : 1,
      limit: req.query.limit ? parseInt(req.query.limit as string) : 20
    };

    const result = await this.productRepo.findAll(filters, sort, pagination);

    res.json(result);
  }
}

// Example API Request:
// GET /api/products?category=electronics&minPrice=100&maxPrice=1000&brand=Apple&inStock=true&search=iphone&sortBy=price&sortOrder=asc&page=1&limit=20
```

**Deliverable:**
- Pagination implementation (offset and cursor-based)
- Filtering logic
- Sorting implementation
- Performance optimization (indexes)

---

### Step 22: File Upload and Processing

**What to do:**
- Design file upload flow
- Implement file validation
- Plan file storage
- Implement file processing (resize, compress)
- Generate thumbnails

**Example:**
```typescript
// File Upload DTO
interface FileUploadDTO {
  file: Express.Multer.File;
  userId: string;
  category: 'avatar' | 'document' | 'product-image';
}

// File Metadata
interface FileMetadata {
  id: string;
  userId: string;
  originalName: string;
  fileName: string;
  mimeType: string;
  size: number;
  url: string;
  thumbnailUrl?: string;
  category: string;
  uploadedAt: Date;
}

// File Validation
class FileValidator {
  private static readonly ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
  private static readonly ALLOWED_DOCUMENT_TYPES = ['application/pdf', 'application/msword'];
  private static readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private static readonly MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

  static validateImage(file: Express.Multer.File): void {
    if (!this.ALLOWED_IMAGE_TYPES.includes(file.mimetype)) {
      throw new ValidationError('Invalid file type. Only JPEG, PNG, GIF, WebP allowed');
    }

    if (file.size > this.MAX_IMAGE_SIZE) {
      throw new ValidationError('File too large. Maximum 5MB allowed');
    }
  }

  static validateDocument(file: Express.Multer.File): void {
    if (!this.ALLOWED_DOCUMENT_TYPES.includes(file.mimetype)) {
      throw new ValidationError('Invalid file type. Only PDF, DOC allowed');
    }

    if (file.size > this.MAX_FILE_SIZE) {
      throw new ValidationError('File too large. Maximum 10MB allowed');
    }
  }
}

// File Storage Service
interface IFileStorageService {
  upload(file: Buffer, fileName: string, mimeType: string): Promise<string>;
  delete(fileName: string): Promise<void>;
  getSignedUrl(fileName: string, expiresIn: number): Promise<string>;
}

class S3FileStorageService implements IFileStorageService {
  constructor(
    private s3: S3Client,
    private bucketName: string
  ) {}

  async upload(file: Buffer, fileName: string, mimeType: string): Promise<string> {
    const command = new PutObjectCommand({
      Bucket: this.bucketName,
      Key: fileName,
      Body: file,
      ContentType: mimeType,
      ACL: 'public-read' // or 'private' for private files
    });

    await this.s3.send(command);

    return `https://${this.bucketName}.s3.amazonaws.com/${fileName}`;
  }

  async delete(fileName: string): Promise<void> {
    const command = new DeleteObjectCommand({
      Bucket: this.bucketName,
      Key: fileName
    });

    await this.s3.send(command);
  }

  async getSignedUrl(fileName: string, expiresIn: number = 3600): Promise<string> {
    const command = new GetObjectCommand({
      Bucket: this.bucketName,
      Key: fileName
    });

    return await getSignedUrl(this.s3, command, { expiresIn });
  }
}

// Image Processing Service
class ImageProcessingService {
  async resize(buffer: Buffer, width: number, height: number): Promise<Buffer> {
    return sharp(buffer)
      .resize(width, height, {
        fit: 'cover',
        position: 'center'
      })
      .toBuffer();
  }

  async compress(buffer: Buffer, quality: number = 80): Promise<Buffer> {
    return sharp(buffer)
      .jpeg({ quality })
      .toBuffer();
  }

  async generateThumbnail(buffer: Buffer): Promise<Buffer> {
    return sharp(buffer)
      .resize(200, 200, {
        fit: 'cover',
        position: 'center'
      })
      .jpeg({ quality: 70 })
      .toBuffer();
  }

  async convertToWebP(buffer: Buffer): Promise<Buffer> {
    return sharp(buffer)
      .webp({ quality: 80 })
      .toBuffer();
  }
}

// File Upload Service
class FileUploadService {
  constructor(
    private storageService: IFileStorageService,
    private imageProcessor: ImageProcessingService,
    private fileRepo: IFileRepository
  ) {}

  async uploadImage(dto: FileUploadDTO): Promise<FileMetadata> {
    // Validate
    FileValidator.validateImage(dto.file);

    // Generate unique filename
    const ext = path.extname(dto.file.originalname);
    const fileName = `${dto.category}/${dto.userId}/${uuidv4()}${ext}`;

    // Process image
    let processedImage = dto.file.buffer;

    // Resize if too large
    const metadata = await sharp(processedImage).metadata();
    if (metadata.width! > 2000 || metadata.height! > 2000) {
      processedImage = await this.imageProcessor.resize(processedImage, 2000, 2000);
    }

    // Compress
    processedImage = await this.imageProcessor.compress(processedImage);

    // Upload original
    const url = await this.storageService.upload(
      processedImage,
      fileName,
      dto.file.mimetype
    );

    // Generate and upload thumbnail
    const thumbnail = await this.imageProcessor.generateThumbnail(dto.file.buffer);
    const thumbnailFileName = `${dto.category}/${dto.userId}/thumbnails/${uuidv4()}${ext}`;
    const thumbnailUrl = await this.storageService.upload(
      thumbnail,
      thumbnailFileName,
      dto.file.mimetype
    );

    // Save metadata to database
    const fileMetadata: FileMetadata = {
      id: uuidv4(),
      userId: dto.userId,
      originalName: dto.file.originalname,
      fileName,
      mimeType: dto.file.mimetype,
      size: processedImage.length,
      url,
      thumbnailUrl,
      category: dto.category,
      uploadedAt: new Date()
    };

    await this.fileRepo.create(fileMetadata);

    return fileMetadata;
  }

  async deleteFile(fileId: string, userId: string): Promise<void> {
    const file = await this.fileRepo.findById(fileId);

    if (!file) {
      throw new NotFoundError('File not found');
    }

    if (file.userId !== userId) {
      throw new AuthorizationError('Not authorized to delete this file');
    }

    // Delete from storage
    await this.storageService.delete(file.fileName);

    if (file.thumbnailUrl) {
      const thumbnailFileName = this.extractFileNameFromUrl(file.thumbnailUrl);
      await this.storageService.delete(thumbnailFileName);
    }

    // Delete from database
    await this.fileRepo.delete(fileId);
  }

  private extractFileNameFromUrl(url: string): string {
    const urlObj = new URL(url);
    return urlObj.pathname.substring(1); // Remove leading slash
  }
}

// Controller
class FileUploadController {
  constructor(private fileUploadService: FileUploadService) {}

  async uploadImage(req: Request, res: Response): Promise<void> {
    if (!req.file) {
      throw new ValidationError('No file uploaded');
    }

    const dto: FileUploadDTO = {
      file: req.file,
      userId: req.user!.id,
      category: req.body.category || 'document'
    };

    const result = await this.fileUploadService.uploadImage(dto);

    res.status(201).json(result);
  }

  async deleteFile(req: Request, res: Response): Promise<void> {
    const { fileId } = req.params;
    const userId = req.user!.id;

    await this.fileUploadService.deleteFile(fileId, userId);

    res.status(204).send();
  }
}

// Multer Configuration
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB
  },
  fileFilter: (req, file, cb) => {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf'];
    if (allowedTypes.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('Invalid file type'));
    }
  }
});

// Routes
router.post('/upload', authMiddleware, upload.single('file'), fileUploadController.uploadImage);
router.delete('/files/:fileId', authMiddleware, fileUploadController.deleteFile);
```

**Deliverable:**
- File upload implementation
- File validation
- Image processing
- Storage integration
- Metadata management

---

### Step 23: Background Jobs and Scheduling

**What to do:**
- Design job queue system
- Implement job processors
- Plan job scheduling (cron jobs)
- Handle job failures and retries
- Monitor job execution

**Example:**
```typescript
// Job Interface
interface Job {
  id: string;
  type: string;
  data: any;
  priority: number;
  attempts: number;
  maxAttempts: number;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  createdAt: Date;
  processedAt?: Date;
  error?: string;
}

// Job Queue Interface
interface IJobQueue {
  add(jobType: string, data: any, options?: JobOptions): Promise<Job>;
  process(jobType: string, handler: JobHandler): void;
}

interface JobOptions {
  priority?: number;
  delay?: number;
  attempts?: number;
}

type JobHandler = (job: Job) => Promise<void>;

// Bull Queue Implementation
class BullJobQueue implements IJobQueue {
  private queues = new Map<string, Queue>();

  constructor(private redis: Redis) {}

  async add(jobType: string, data: any, options: JobOptions = {}): Promise<Job> {
    const queue = this.getOrCreateQueue(jobType);

    const bullJob = await queue.add(data, {
      priority: options.priority || 0,
      delay: options.delay || 0,
      attempts: options.attempts || 3,
      backoff: {
        type: 'exponential',
        delay: 2000
      }
    });

    return {
      id: bullJob.id!.toString(),
      type: jobType,
      data: bullJob.data,
      priority: options.priority || 0,
      attempts: 0,
      maxAttempts: options.attempts || 3,
      status: 'pending',
      createdAt: new Date()
    };
  }

  process(jobType: string, handler: JobHandler): void {
    const queue = this.getOrCreateQueue(jobType);

    queue.process(async (bullJob) => {
      const job: Job = {
        id: bullJob.id!.toString(),
        type: jobType,
        data: bullJob.data,
        priority: 0,
        attempts: bullJob.attemptsMade,
        maxAttempts: bullJob.opts.attempts || 3,
        status: 'processing',
        createdAt: new Date(bullJob.timestamp)
      };

      await handler(job);
    });
  }

  private getOrCreateQueue(jobType: string): Queue {
    if (!this.queues.has(jobType)) {
      this.queues.set(jobType, new Queue(jobType, {
        redis: this.redis
      }));
    }
    return this.queues.get(jobType)!;
  }
}

// Specific Job Handlers
class EmailJobHandler {
  constructor(private emailService: IEmailService) {}

  async handle(job: Job): Promise<void> {
    const { to, subject, body } = job.data;

    try {
      await this.emailService.sendEmail(to, subject, body);
      logger.info('Email sent successfully', { jobId: job.id, to });
    } catch (error) {
      logger.error('Failed to send email', { jobId: job.id, error });
      throw error; // Will trigger retry
    }
  }
}

class ReportGenerationJobHandler {
  constructor(
    private reportService: IReportService,
    private storageService: IFileStorageService
  ) {}

  async handle(job: Job): Promise<void> {
    const { userId, reportType, filters } = job.data;

    try {
      // Generate report
      const report = await this.reportService.generate(reportType, filters);

      // Upload to storage
      const fileName = `reports/${userId}/${Date.now()}-${reportType}.pdf`;
      const url = await this.storageService.upload(
        report,
        fileName,
        'application/pdf'
      );

      // Notify user
      await this.emailService.sendEmail(
        userId,
        'Report Ready',
        `Your report is ready: ${url}`
      );

      logger.info('Report generated successfully', { jobId: job.id, userId });
    } catch (error) {
      logger.error('Failed to generate report', { jobId: job.id, error });
      throw error;
    }
  }
}

// Job Scheduler (Cron Jobs)
class JobScheduler {
  private scheduledJobs = new Map<string, cron.ScheduledTask>();

  constructor(private jobQueue: IJobQueue) {}

  schedule(name: string, cronExpression: string, jobType: string, data: any): void {
    const task = cron.schedule(cronExpression, async () => {
      logger.info('Scheduled job triggered', { name, jobType });
      await this.jobQueue.add(jobType, data);
    });

    this.scheduledJobs.set(name, task);
    logger.info('Job scheduled', { name, cronExpression, jobType });
  }

  unschedule(name: string): void {
    const task = this.scheduledJobs.get(name);
    if (task) {
      task.stop();
      this.scheduledJobs.delete(name);
      logger.info('Job unscheduled', { name });
    }
  }
}

// Usage
const jobQueue = new BullJobQueue(redis);
const emailJobHandler = new EmailJobHandler(emailService);
const reportJobHandler = new ReportGenerationJobHandler(reportService, storageService);

// Register job processors
jobQueue.process('send-email', (job) => emailJobHandler.handle(job));
jobQueue.process('generate-report', (job) => reportJobHandler.handle(job));

// Add jobs
await jobQueue.add('send-email', {
  to: 'user@example.com',
  subject: 'Welcome',
  body: 'Welcome to our platform!'
});

await jobQueue.add('generate-report', {
  userId: 'user-123',
  reportType: 'sales',
  filters: { startDate: '2026-01-01', endDate: '2026-03-07' }
}, {
  priority: 10,
  delay: 5000 // 5 seconds delay
});

// Schedule recurring jobs
const scheduler = new JobScheduler(jobQueue);

// Daily report at 2 AM
scheduler.schedule('daily-report', '0 2 * * *', 'generate-report', {
  reportType: 'daily-summary'
});

// Cleanup old data every Sunday at 3 AM
scheduler.schedule('weekly-cleanup', '0 3 * * 0', 'cleanup-data', {});

// Send reminder emails every hour
scheduler.schedule('hourly-reminders', '0 * * * *', 'send-reminders', {});
```

**Deliverable:**
- Job queue implementation
- Job handlers
- Scheduling configuration
- Retry and error handling

---

### Step 24: WebSocket and Real-time Communication

**What to do:**
- Design WebSocket connection management
- Implement event handlers
- Plan room/channel management
- Handle authentication
- Implement presence system

**Example:**
```typescript
// WebSocket Event Types
enum WebSocketEvent {
  CONNECT = 'connect',
  DISCONNECT = 'disconnect',
  MESSAGE_SEND = 'message:send',
  MESSAGE_RECEIVE = 'message:receive',
  TYPING_START = 'typing:start',
  TYPING_STOP = 'typing:stop',
  USER_ONLINE = 'user:online',
  USER_OFFLINE = 'user:offline',
  ROOM_JOIN = 'room:join',
  ROOM_LEAVE = 'room:leave'
}

// WebSocket Message Interface
interface WebSocketMessage {
  event: WebSocketEvent;
  data: any;
  timestamp: Date;
}

// Connection Manager
class WebSocketConnectionManager {
  private connections = new Map<string, Socket>(); // userId -> socket
  private userRooms = new Map<string, Set<string>>(); // userId -> Set<roomId>

  addConnection(userId: string, socket: Socket): void {
    this.connections.set(userId, socket);
    this.userRooms.set(userId, new Set());
    logger.info('User connected', { userId, socketId: socket.id });
  }

  removeConnection(userId: string): void {
    const socket = this.connections.get(userId);
    if (socket) {
      // Leave all rooms
      const rooms = this.userRooms.get(userId);
      if (rooms) {
        rooms.forEach(roomId => {
          socket.leave(roomId);
        });
      }

      this.connections.delete(userId);
      this.userRooms.delete(userId);
      logger.info('User disconnected', { userId });
    }
  }

  getConnection(userId: string): Socket | undefined {
    return this.connections.get(userId);
  }

  isOnline(userId: string): boolean {
    return this.connections.has(userId);
  }

  joinRoom(userId: string, roomId: string): void {
    const socket = this.connections.get(userId);
    if (socket) {
      socket.join(roomId);
      this.userRooms.get(userId)?.add(roomId);
      logger.info('User joined room', { userId, roomId });
    }
  }

  leaveRoom(userId: string, roomId: string): void {
    const socket = this.connections.get(userId);
    if (socket) {
      socket.leave(roomId);
      this.userRooms.get(userId)?.delete(roomId);
      logger.info('User left room', { userId, roomId });
    }
  }

  getUsersInRoom(roomId: string): string[] {
    const users: string[] = [];
    this.userRooms.forEach((rooms, userId) => {
      if (rooms.has(roomId)) {
        users.push(userId);
      }
    });
    return users;
  }
}

// WebSocket Service
class WebSocketService {
  private io: Server;
  private connectionManager: WebSocketConnectionManager;

  constructor(
    httpServer: HttpServer,
    private messageService: IMessageService,
    private presenceService: IPresenceService
  ) {
    this.io = new Server(httpServer, {
      cors: {
        origin: process.env.FRONTEND_URL,
        credentials: true
      }
    });

    this.connectionManager = new WebSocketConnectionManager();
    this.setupMiddleware();
    this.setupEventHandlers();
  }

  private setupMiddleware(): void {
    // Authentication middleware
    this.io.use(async (socket, next) => {
      try {
        const token = socket.handshake.auth.token;
        if (!token) {
          throw new Error('Authentication token required');
        }

        const decoded = await verifyToken(token);
        socket.data.userId = decoded.userId;
        next();
      } catch (error) {
        next(new Error('Authentication failed'));
      }
    });
  }

  private setupEventHandlers(): void {
    this.io.on(WebSocketEvent.CONNECT, (socket: Socket) => {
      const userId = socket.data.userId;

      // Add connection
      this.connectionManager.addConnection(userId, socket);

      // Update presence
      this.presenceService.setOnline(userId);

      // Notify others
      socket.broadcast.emit(WebSocketEvent.USER_ONLINE, { userId });

      // Handle disconnect
      socket.on(WebSocketEvent.DISCONNECT, () => {
        this.handleDisconnect(userId, socket);
      });

      // Handle message send
      socket.on(WebSocketEvent.MESSAGE_SEND, async (data) => {
        await this.handleMessageSend(userId, data, socket);
      });

      // Handle typing events
      socket.on(WebSocketEvent.TYPING_START, (data) => {
        this.handleTypingStart(userId, data, socket);
      });

      socket.on(WebSocketEvent.TYPING_STOP, (data) => {
        this.handleTypingStop(userId, data, socket);
      });

      // Handle room join
      socket.on(WebSocketEvent.ROOM_JOIN, (data) => {
        this.handleRoomJoin(userId, data.roomId, socket);
      });

      // Handle room leave
      socket.on(WebSocketEvent.ROOM_LEAVE, (data) => {
        this.handleRoomLeave(userId, data.roomId, socket);
      });
    });
  }

  private handleDisconnect(userId: string, socket: Socket): void {
    this.connectionManager.removeConnection(userId);
    this.presenceService.setOffline(userId);
    socket.broadcast.emit(WebSocketEvent.USER_OFFLINE, { userId });
  }

  private async handleMessageSend(userId: string, data: any, socket: Socket): Promise<void> {
    try {
      // Save message to database
      const message = await this.messageService.create({
        senderId: userId,
        receiverId: data.receiverId,
        content: data.content,
        conversationId: data.conversationId
      });

      // Send to receiver if online
      const receiverSocket = this.connectionManager.getConnection(data.receiverId);
      if (receiverSocket) {
        receiverSocket.emit(WebSocketEvent.MESSAGE_RECEIVE, message);
      }

      // Acknowledge to sender
      socket.emit(WebSocketEvent.MESSAGE_RECEIVE, message);

      logger.info('Message sent', { messageId: message.id, senderId: userId });
    } catch (error) {
      logger.error('Failed to send message', { error });
      socket.emit('error', { message: 'Failed to send message' });
    }
  }

  private handleTypingStart(userId: string, data: any, socket: Socket): void {
    const receiverSocket = this.connectionManager.getConnection(data.receiverId);
    if (receiverSocket) {
      receiverSocket.emit(WebSocketEvent.TYPING_START, {
        userId,
        conversationId: data.conversationId
      });
    }
  }

  private handleTypingStop(userId: string, data: any, socket: Socket): void {
    const receiverSocket = this.connectionManager.getConnection(data.receiverId);
    if (receiverSocket) {
      receiverSocket.emit(WebSocketEvent.TYPING_STOP, {
        userId,
        conversationId: data.conversationId
      });
    }
  }

  private handleRoomJoin(userId: string, roomId: string, socket: Socket): void {
    this.connectionManager.joinRoom(userId, roomId);

    // Notify others in room
    socket.to(roomId).emit(WebSocketEvent.USER_ONLINE, { userId, roomId });

    // Send current users in room
    const users = this.connectionManager.getUsersInRoom(roomId);
    socket.emit('room:users', { roomId, users });
  }

  private handleRoomLeave(userId: string, roomId: string, socket: Socket): void {
    this.connectionManager.leaveRoom(userId, roomId);

    // Notify others in room
    socket.to(roomId).emit(WebSocketEvent.USER_OFFLINE, { userId, roomId });
  }

  // Broadcast to room
  broadcastToRoom(roomId: string, event: string, data: any): void {
    this.io.to(roomId).emit(event, data);
  }

  // Send to specific user
  sendToUser(userId: string, event: string, data: any): void {
    const socket = this.connectionManager.getConnection(userId);
    if (socket) {
      socket.emit(event, data);
    }
  }
}

// Presence Service
class PresenceService {
  constructor(private redis: Redis) {}

  async setOnline(userId: string): Promise<void> {
    await this.redis.hset('presence', userId, Date.now().toString());
    await this.redis.expire(`presence:${userId}`, 300); // 5 minutes
  }

  async setOffline(userId: string): Promise<void> {
    await this.redis.hdel('presence', userId);
  }

  async isOnline(userId: string): Promise<boolean> {
    const timestamp = await this.redis.hget('presence', userId);
    return timestamp !== null;
  }

  async getOnlineUsers(): Promise<string[]> {
    const presence = await this.redis.hgetall('presence');
    return Object.keys(presence);
  }
}

// Client-side usage example
const socket = io('http://localhost:3000', {
  auth: {
    token: 'jwt-token-here'
  }
});

socket.on('connect', () => {
  console.log('Connected');

  // Join a room
  socket.emit('room:join', { roomId: 'room-123' });
});

socket.on('message:receive', (message) => {
  console.log('New message:', message);
});

socket.on('typing:start', (data) => {
  console.log('User is typing:', data.userId);
});

// Send message
socket.emit('message:send', {
  receiverId: 'user-456',
  content: 'Hello!',
  conversationId: 'conv-789'
});
```

**Deliverable:**
- WebSocket server implementation
- Connection management
- Event handlers
- Presence system
- Room/channel management

---

### Step 25: Rate Limiting Implementation

**What to do:**
- Implement rate limiting middleware
- Choose algorithm (Token Bucket, Sliding Window)
- Store rate limit data (Redis)
- Handle rate limit exceeded
- Different limits for different users

**Example:**
```typescript
// Rate Limit Configuration
interface RateLimitConfig {
  windowMs: number; // Time window in milliseconds
  maxRequests: number; // Max requests in window
  message?: string;
  skipSuccessfulRequests?: boolean;
  skipFailedRequests?: boolean;
}

// Rate Limiter Interface
interface IRateLimiter {
  checkLimit(key: string, config: RateLimitConfig): Promise<RateLimitResult>;
}

interface RateLimitResult {
  allowed: boolean;
  remaining: number;
  resetAt: Date;
}

// Token Bucket Rate Limiter
class TokenBucketRateLimiter implements IRateLimiter {
  constructor(private redis: Redis) {}

  async checkLimit(key: string, config: RateLimitConfig): Promise<RateLimitResult> {
    const now = Date.now();
    const windowKey = `ratelimit:${key}`;

    // Get current tokens
    const data = await this.redis.get(windowKey);
    let tokens = config.maxRequests;
    let lastRefill = now;

    if (data) {
      const parsed = JSON.parse(data);
      tokens = parsed.tokens;
      lastRefill = parsed.lastRefill;

      // Refill tokens based on time passed
      const timePassed = now - lastRefill;
      const tokensToAdd = Math.floor(timePassed / config.windowMs) * config.maxRequests;
      tokens = Math.min(config.maxRequests, tokens + tokensToAdd);
      lastRefill = now;
    }

    // Check if request allowed
    const allowed = tokens > 0;

    if (allowed) {
      tokens--;
    }

    // Save state
    await this.redis.setex(
      windowKey,
      Math.ceil(config.windowMs / 1000),
      JSON.stringify({ tokens, lastRefill })
    );

    return {
      allowed,
      remaining: tokens,
      resetAt: new Date(lastRefill + config.windowMs)
    };
  }
}

// Sliding Window Rate Limiter
class SlidingWindowRateLimiter implements IRateLimiter {
  constructor(private redis: Redis) {}

  async checkLimit(key: string, config: RateLimitConfig): Promise<RateLimitResult> {
    const now = Date.now();
    const windowKey = `ratelimit:${key}`;
    const windowStart = now - config.windowMs;

    // Remove old entries
    await this.redis.zremrangebyscore(windowKey, 0, windowStart);

    // Count requests in window
    const count = await this.redis.zcard(windowKey);

    // Check if allowed
    const allowed = count < config.maxRequests;

    if (allowed) {
      // Add current request
      await this.redis.zadd(windowKey, now, `${now}-${Math.random()}`);
      await this.redis.expire(windowKey, Math.ceil(config.windowMs / 1000));
    }

    // Get oldest request time for reset calculation
    const oldest = await this.redis.zrange(windowKey, 0, 0, 'WITHSCORES');
    const resetAt = oldest.length > 0
      ? new Date(parseInt(oldest[1]) + config.windowMs)
      : new Date(now + config.windowMs);

    return {
      allowed,
      remaining: Math.max(0, config.maxRequests - count - (allowed ? 1 : 0)),
      resetAt
    };
  }
}

// Rate Limit Middleware
class RateLimitMiddleware {
  constructor(private rateLimiter: IRateLimiter) {}

  create(config: RateLimitConfig) {
    return async (req: Request, res: Response, next: NextFunction) => {
      try {
        // Generate key based on user or IP
        const key = this.generateKey(req);

        // Check rate limit
        const result = await this.rateLimiter.checkLimit(key, config);

        // Set headers
        res.setHeader('X-RateLimit-Limit', config.maxRequests);
        res.setHeader('X-RateLimit-Remaining', result.remaining);
        res.setHeader('X-RateLimit-Reset', result.resetAt.toISOString());

        if (!result.allowed) {
          res.status(429).json({
            error: 'TOO_MANY_REQUESTS',
            message: config.message || 'Too many requests, please try again later',
            retryAfter: result.resetAt
          });
          return;
        }

        next();
      } catch (error) {
        logger.error('Rate limit check failed', { error });
        // Fail open - allow request if rate limiter fails
        next();
      }
    };
  }

  private generateKey(req: Request): string {
    // Use user ID if authenticated, otherwise IP
    if (req.user) {
      return `user:${req.user.id}`;
    }
    return `ip:${req.ip}`;
  }
}

// Usage
const rateLimiter = new SlidingWindowRateLimiter(redis);
const rateLimitMiddleware = new RateLimitMiddleware(rateLimiter);

// Apply to routes
app.use('/api/', rateLimitMiddleware.create({
  windowMs: 15 * 60 * 1000, // 15 minutes
  maxRequests: 100,
  message: 'Too many requests from this IP'
}));

// Stricter limit for auth endpoints
app.use('/api/auth/login', rateLimitMiddleware.create({
  windowMs: 15 * 60 * 1000,
  maxRequests: 5,
  message: 'Too many login attempts'
}));

// Different limits for different user tiers
const createTieredRateLimit = (req: Request, res: Response, next: NextFunction) => {
  const user = req.user;

  let config: RateLimitConfig;

  if (!user) {
    // Anonymous users
    config = {
      windowMs: 60 * 60 * 1000, // 1 hour
      maxRequests: 100
    };
  } else if (user.tier === 'premium') {
    // Premium users
    config = {
      windowMs: 60 * 60 * 1000,
      maxRequests: 10000
    };
  } else {
    // Regular users
    config = {
      windowMs: 60 * 60 * 1000,
      maxRequests: 1000
    };
  }

  return rateLimitMiddleware.create(config)(req, res, next);
};

app.use('/api/products', createTieredRateLimit);
```

**Deliverable:**
- Rate limiter implementation
- Middleware integration
- Configuration per endpoint
- User tier support

---

**Remember:** LLD bridges design and code. It should be detailed enough to guide implementation but flexible enough to accommodate improvements!

