# Low-Level Design (LLD) - Simplified Guide with Short Examples

## 📚 Purpose
This is a simplified version of the LLD guide with concise examples and technology explanations. Perfect for quick reference and learning.

**Note:** For detailed explanations of any technology, refer to `Technology_Stack_Glossary.md`

---

## 1. Module Structure

**What to do:** Organize your code into logical folders/packages

**Technology Used:**
- **Node.js/TypeScript** - Project structure
  - *What it is:* Organized folder hierarchy for code
  - *Why:* Easy to find files, separation of concerns

**Example:**
```
/src
  /controllers    # Handle HTTP requests
  /services       # Business logic
  /repositories   # Database access
  /models         # Data structures
  /middleware     # Request interceptors
  /utils          # Helper functions
  /config         # Configuration
```

**Key Points:**
- Each folder has a specific responsibility
- Controllers → Services → Repositories (layered architecture)
- Easy to test each layer independently

---

## 2. Class Design

**What to do:** Define classes with attributes and methods

**Technology Used:**
- **TypeScript** - Type-safe JavaScript
  - *What it is:* JavaScript with types
  - *Why:* Catch errors early, better IDE support

**Example:**
```typescript
// User Model
class User {
  id: string;
  email: string;
  passwordHash: string;
  role: 'admin' | 'user';
  createdAt: Date;
}

// User Service
class UserService {
  constructor(private userRepo: UserRepository) {}
  
  async createUser(email: string, password: string): Promise<User> {
    // Business logic here
  }
  
  async findById(id: string): Promise<User | null> {
    return this.userRepo.findById(id);
  }
}
```

**Key Points:**
- Class = Blueprint for objects
- Attributes = Data (properties)
- Methods = Actions (functions)
- Use dependency injection (pass dependencies in constructor)

---

## 3. Database Schema

**What to do:** Design tables with columns, types, and relationships

**Technology Used:**
- **PostgreSQL** - Relational database
  - *What it is:* SQL database with ACID compliance
  - *Why:* Data integrity, complex queries, relationships

**Example:**
```sql
-- Users table
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(50) DEFAULT 'user',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast email lookup
CREATE INDEX idx_users_email ON users(email);

-- Orders table with foreign key
CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  total DECIMAL(10, 2) NOT NULL,
  status VARCHAR(50) DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Key Points:**
- **Primary Key:** Unique identifier (id)
- **Foreign Key:** Links to another table (user_id → users.id)
- **Index:** Speeds up searches
- **Constraints:** NOT NULL, UNIQUE, DEFAULT

---

## 4. API Specification

**What to do:** Define endpoints with request/response formats

**Technology Used:**
- **REST API** - Standard API architecture
  - *What it is:* HTTP-based API using GET, POST, PUT, DELETE
  - *Why:* Industry standard, easy to understand

**Example:**
```
POST /api/v1/users/register

Request:
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

Success Response (201 Created):
{
  "id": "uuid-123",
  "email": "user@example.com",
  "role": "user",
  "token": "jwt-token-here"
}

Error Response (400 Bad Request):
{
  "error": "VALIDATION_ERROR",
  "message": "Email already exists"
}
```

**HTTP Status Codes:**
- **200 OK** - Success
- **201 Created** - Resource created
- **400 Bad Request** - Invalid input
- **401 Unauthorized** - Not authenticated
- **404 Not Found** - Resource doesn't exist
- **500 Internal Server Error** - Server error

---

## 5. Algorithms and Logic

**What to do:** Write pseudocode for complex operations

**Example: User Registration**
```
FUNCTION registerUser(email, password):
  1. Validate email format
     IF not valid THEN throw ValidationError
  
  2. Check if email exists
     user = database.findByEmail(email)
     IF user exists THEN throw ValidationError("Email exists")
  
  3. Validate password strength
     IF password too weak THEN throw ValidationError
  
  4. Hash password
     passwordHash = bcrypt.hash(password, saltRounds=10)
  
  5. Create user in database
     user = database.create({
       email: email,
       passwordHash: passwordHash,
       role: 'user'
     })
  
  6. Generate JWT token
     token = jwt.sign({ userId: user.id }, secret, expiresIn='15m')
  
  7. Return user and token
     RETURN { user, token }
END FUNCTION
```

**Key Points:**
- Break down into clear steps
- Handle errors at each step
- Easy to convert to actual code

---

## 6. Error Handling

**What to do:** Define error types and how to handle them

**Technology Used:**
- **Custom Error Classes** - Specific error types
  - *Why:* Better error handling, clear error messages

**Example:**
```typescript
// Base error class
class ApplicationError extends Error {
  constructor(
    public message: string,
    public statusCode: number,
    public errorCode: string
  ) {
    super(message);
  }
}

// Specific error types
class ValidationError extends ApplicationError {
  constructor(message: string) {
    super(message, 400, 'VALIDATION_ERROR');
  }
}

class AuthenticationError extends ApplicationError {
  constructor(message: string = 'Invalid credentials') {
    super(message, 401, 'AUTHENTICATION_ERROR');
  }
}

class NotFoundError extends ApplicationError {
  constructor(resource: string) {
    super(`${resource} not found`, 404, 'NOT_FOUND');
  }
}

// Usage
if (!user) {
  throw new NotFoundError('User');
}
```

**Error Response Format:**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Email already exists",
  "statusCode": 400
}
```

---

## 7. Authentication Implementation

**What to do:** Implement login/logout with JWT tokens

**Technology Used:**
- **JWT (JSON Web Token)** - Stateless authentication
  - *What it is:* Encoded token with user info
  - *Why:* No server-side session storage needed
- **bcrypt** - Password hashing
  - *What it is:* Secure one-way hash function
  - *Why:* Can't reverse hash to get password

**Example:**
```typescript
class AuthService {
  // Login
  async login(email: string, password: string) {
    // 1. Find user
    const user = await this.userRepo.findByEmail(email);
    if (!user) throw new AuthenticationError();
    
    // 2. Compare password
    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) throw new AuthenticationError();
    
    // 3. Generate token
    const token = jwt.sign(
      { userId: user.id, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: '15m' }
    );
    
    return { user, token };
  }
  
  // Verify token (middleware)
  verifyToken(token: string) {
    return jwt.verify(token, process.env.JWT_SECRET);
  }
}
```

**Authentication Flow:**
```
1. User sends email + password
2. Server verifies credentials
3. Server generates JWT token
4. Client stores token
5. Client sends token with each request
6. Server verifies token
```

---

## 8. Caching Implementation

**What to do:** Store frequently accessed data in memory

**Technology Used:**
- **Redis** - In-memory cache
  - *What it is:* Ultra-fast key-value store
  - *Why:* Sub-millisecond response time

**Example:**
```typescript
class CacheService {
  constructor(private redis: Redis) {}
  
  // Get from cache
  async get<T>(key: string): Promise<T | null> {
    const value = await this.redis.get(key);
    return value ? JSON.parse(value) : null;
  }
  
  // Set in cache with TTL (Time To Live)
  async set<T>(key: string, value: T, ttlSeconds: number): Promise<void> {
    await this.redis.setex(key, ttlSeconds, JSON.stringify(value));
  }
  
  // Delete from cache
  async delete(key: string): Promise<void> {
    await this.redis.del(key);
  }
}

// Usage: Cache-Aside Pattern
class ProductService {
  async getProduct(id: string): Promise<Product> {
    const cacheKey = `product:${id}`;
    
    // Try cache first
    let product = await this.cache.get<Product>(cacheKey);
    if (product) return product; // Cache hit
    
    // Cache miss - get from database
    product = await this.productRepo.findById(id);
    
    // Store in cache for 1 hour
    if (product) {
      await this.cache.set(cacheKey, product, 3600);
    }
    
    return product;
  }
}
```

**Cache Strategy:**
- **Cache Hit:** Data found in cache (fast)
- **Cache Miss:** Data not in cache, fetch from DB (slow)
- **TTL:** Time before cache expires (e.g., 1 hour)

---

## 9. Database Transactions

**What to do:** Ensure multiple operations succeed or fail together

**Technology Used:**
- **PostgreSQL Transactions** - ACID compliance
  - *What it is:* Group of operations that execute as one unit
  - *Why:* Data consistency, rollback on error

**Example:**
```typescript
class OrderService {
  async createOrder(userId: string, items: CartItem[]) {
    // Start transaction
    const client = await this.db.connect();
    
    try {
      await client.query('BEGIN');
      
      // 1. Create order
      const order = await client.query(
        'INSERT INTO orders (user_id, total) VALUES ($1, $2) RETURNING *',
        [userId, calculateTotal(items)]
      );
      
      // 2. Create order items
      for (const item of items) {
        await client.query(
          'INSERT INTO order_items (order_id, product_id, quantity) VALUES ($1, $2, $3)',
          [order.id, item.productId, item.quantity]
        );
      }
      
      // 3. Update inventory
      for (const item of items) {
        await client.query(
          'UPDATE products SET inventory = inventory - $1 WHERE id = $2',
          [item.quantity, item.productId]
        );
      }
      
      // Commit transaction
      await client.query('COMMIT');
      return order;
      
    } catch (error) {
      // Rollback on error
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
}
```

**Key Points:**
- **BEGIN:** Start transaction
- **COMMIT:** Save all changes
- **ROLLBACK:** Undo all changes if error
- All operations succeed together or fail together

---

## 10. Pagination Implementation

**What to do:** Return large datasets in chunks

**Technology Used:**
- **Offset-based Pagination** - Simple page numbers
  - *What it is:* Skip N records, take M records
  - *Why:* Easy to implement, familiar to users

**Example:**
```typescript
interface PaginationParams {
  page: number;      // Current page (1, 2, 3...)
  limit: number;     // Items per page (10, 20, 50...)
}

class ProductRepository {
  async findAll(page: number = 1, limit: number = 20) {
    const offset = (page - 1) * limit;
    
    // Get total count
    const countResult = await this.db.query(
      'SELECT COUNT(*) FROM products'
    );
    const total = parseInt(countResult.rows[0].count);
    
    // Get paginated data
    const dataResult = await this.db.query(
      'SELECT * FROM products ORDER BY created_at DESC LIMIT $1 OFFSET $2',
      [limit, offset]
    );
    
    return {
      data: dataResult.rows,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
        hasNext: page * limit < total,
        hasPrevious: page > 1
      }
    };
  }
}
```

**Response Example:**
```json
{
  "data": [...],
  "pagination": {
    "page": 2,
    "limit": 20,
    "total": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": true
  }
}
```

---

## 11. File Upload

**What to do:** Handle file uploads and store them

**Technology Used:**
- **Multer** - File upload middleware for Node.js
  - *What it is:* Handles multipart/form-data
  - *Why:* Easy file handling, validation
- **AWS S3** - Cloud storage
  - *What it is:* Scalable object storage
  - *Why:* Unlimited storage, CDN integration

**Example:**
```typescript
// Multer configuration
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
  fileFilter: (req, file, cb) => {
    const allowed = ['image/jpeg', 'image/png'];
    if (allowed.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('Invalid file type'));
    }
  }
});

// Upload service
class FileUploadService {
  async uploadImage(file: Express.Multer.File, userId: string) {
    // 1. Validate file
    if (!file) throw new ValidationError('No file provided');
    
    // 2. Generate unique filename
    const filename = `${userId}/${Date.now()}-${file.originalname}`;
    
    // 3. Upload to S3
    await this.s3.upload({
      Bucket: 'my-bucket',
      Key: filename,
      Body: file.buffer,
      ContentType: file.mimetype
    });
    
    // 4. Return URL
    return `https://my-bucket.s3.amazonaws.com/${filename}`;
  }
}

// Controller
router.post('/upload', upload.single('file'), async (req, res) => {
  const url = await fileUploadService.uploadImage(req.file, req.user.id);
  res.json({ url });
});
```

---

## 12. Background Jobs

**What to do:** Process tasks asynchronously

**Technology Used:**
- **Bull** - Job queue for Node.js
  - *What it is:* Redis-based job queue
  - *Why:* Reliable, retries, scheduling
- **Redis** - Message broker
  - *What it is:* In-memory data store
  - *Why:* Fast, persistent queues

**Example:**
```typescript
// Create queue
const emailQueue = new Bull('email', {
  redis: { host: 'localhost', port: 6379 }
});

// Add job to queue
await emailQueue.add('send-welcome-email', {
  to: 'user@example.com',
  subject: 'Welcome!',
  body: 'Welcome to our platform'
}, {
  attempts: 3,           // Retry 3 times
  backoff: 5000,         // Wait 5s between retries
  delay: 2000            // Start after 2s
});

// Process jobs
emailQueue.process('send-welcome-email', async (job) => {
  const { to, subject, body } = job.data;
  await emailService.send(to, subject, body);
});

// Schedule recurring job (cron)
emailQueue.add('daily-report', {}, {
  repeat: { cron: '0 9 * * *' } // Every day at 9 AM
});
```

**Use Cases:**
- Send emails
- Generate reports
- Process images
- Data cleanup
- Scheduled tasks

---

## 13. WebSocket (Real-time)

**What to do:** Enable real-time bidirectional communication

**Technology Used:**
- **Socket.io** - WebSocket library
  - *What it is:* Real-time event-based communication
  - *Why:* Easy to use, fallback support, rooms

**Example:**
```typescript
// Server
const io = new Server(httpServer);

io.on('connection', (socket) => {
  console.log('User connected:', socket.id);
  
  // Join room
  socket.on('join-room', (roomId) => {
    socket.join(roomId);
  });
  
  // Receive message
  socket.on('send-message', (data) => {
    // Broadcast to room
    io.to(data.roomId).emit('new-message', {
      user: socket.id,
      message: data.message,
      timestamp: new Date()
    });
  });
  
  // Disconnect
  socket.on('disconnect', () => {
    console.log('User disconnected:', socket.id);
  });
});

// Client
const socket = io('http://localhost:3000');

// Join room
socket.emit('join-room', 'room-123');

// Send message
socket.emit('send-message', {
  roomId: 'room-123',
  message: 'Hello!'
});

// Receive message
socket.on('new-message', (data) => {
  console.log('New message:', data);
});
```

**Use Cases:**
- Chat applications
- Real-time notifications
- Live dashboards
- Collaborative editing
- Online games

---

## 14. Rate Limiting

**What to do:** Limit number of requests per user/IP

**Technology Used:**
- **Redis** - Store request counts
  - *Why:* Fast, atomic operations, TTL support

**Example:**
```typescript
class RateLimiter {
  constructor(private redis: Redis) {}
  
  async checkLimit(key: string, maxRequests: number, windowSeconds: number) {
    const current = await this.redis.incr(key);
    
    if (current === 1) {
      // First request, set expiry
      await this.redis.expire(key, windowSeconds);
    }
    
    if (current > maxRequests) {
      return { allowed: false, remaining: 0 };
    }
    
    return { allowed: true, remaining: maxRequests - current };
  }
}

// Middleware
const rateLimitMiddleware = async (req, res, next) => {
  const key = `ratelimit:${req.ip}`;
  const result = await rateLimiter.checkLimit(key, 100, 3600); // 100 req/hour
  
  if (!result.allowed) {
    return res.status(429).json({
      error: 'Too many requests',
      retryAfter: 3600
    });
  }
  
  res.setHeader('X-RateLimit-Remaining', result.remaining);
  next();
};
```

**Rate Limit Examples:**
- **Anonymous users:** 100 requests/hour
- **Authenticated users:** 1000 requests/hour
- **Premium users:** 10,000 requests/hour
- **Login endpoint:** 5 attempts/15 minutes

---

## Quick Reference: Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Language** | TypeScript | Type-safe JavaScript |
| **Framework** | Express.js | Web server |
| **Database** | PostgreSQL | Relational data |
| **Cache** | Redis | Fast data access |
| **Auth** | JWT + bcrypt | Authentication |
| **File Upload** | Multer + S3 | File handling |
| **Jobs** | Bull | Background tasks |
| **Real-time** | Socket.io | WebSocket |
| **API Docs** | Swagger | Documentation |

**For detailed explanations, see:** `Technology_Stack_Glossary.md`

---

## Summary: LLD Best Practices

✅ **Keep it Simple:** Start with simple solutions  
✅ **Use Patterns:** Apply design patterns appropriately  
✅ **Add Comments:** Explain complex logic  
✅ **Handle Errors:** Always handle edge cases  
✅ **Test Everything:** Write unit tests  
✅ **Follow SOLID:** Single responsibility, dependency injection  
✅ **Document APIs:** Use Swagger/OpenAPI  
✅ **Use TypeScript:** Catch errors early  
✅ **Validate Input:** Never trust user input  
✅ **Log Everything:** Use structured logging  

---

**This simplified guide covers all essential LLD topics with concise, practical examples!**

