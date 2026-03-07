# Low-Level Design (LLD) Template
## Component: [Component Name - e.g., Authentication Service]

**Version:** 1.0  
**Date:** 2026-03-07  
**Author:** [Your Name]  
**Status:** Draft / In Review / Approved  
**Related HLD:** [Link to HLD document]

---

## 1. Component Overview

### 1.1 Purpose
[Describe what this component does and why it exists]

Example: The Authentication Service handles user registration, login, logout, and token management. It ensures secure access to the system and manages user sessions.

### 1.2 Responsibilities
- Responsibility 1: User registration with email/password
- Responsibility 2: User authentication (login/logout)
- Responsibility 3: JWT token generation and validation
- Responsibility 4: Password reset functionality
- Responsibility 5: Session management

### 1.3 Dependencies
- **Internal:** User Service (for user profile data)
- **External:** Email Service (SendGrid), Redis (session storage)
- **Database:** PostgreSQL (user credentials)

---

## 2. Module Structure

### 2.1 Package/Folder Structure
```
/auth-service
├── /src
│   ├── /controllers       # HTTP request handlers
│   │   ├── AuthController.ts
│   │   └── UserController.ts
│   ├── /services          # Business logic
│   │   ├── AuthService.ts
│   │   ├── TokenService.ts
│   │   └── PasswordService.ts
│   ├── /repositories      # Data access layer
│   │   └── UserRepository.ts
│   ├── /models            # Data models
│   │   ├── User.ts
│   │   └── Session.ts
│   ├── /middleware        # Express middleware
│   │   ├── authMiddleware.ts
│   │   └── validationMiddleware.ts
│   ├── /validators        # Input validation
│   │   └── authValidators.ts
│   ├── /utils             # Utility functions
│   │   ├── encryption.ts
│   │   └── logger.ts
│   ├── /config            # Configuration
│   │   └── config.ts
│   ├── /types             # TypeScript types
│   │   └── index.ts
│   └── app.ts             # Application entry point
├── /tests
│   ├── /unit
│   └── /integration
├── package.json
└── tsconfig.json
```

---

## 3. Class Design

### 3.1 Class Diagram

```
┌─────────────────────┐
│  AuthController     │
├─────────────────────┤
│ - authService       │
├─────────────────────┤
│ + register()        │
│ + login()           │
│ + logout()          │
│ + refreshToken()    │
└──────────┬──────────┘
           │ uses
           ▼
┌─────────────────────┐
│   AuthService       │
├─────────────────────┤
│ - userRepository    │
│ - tokenService      │
│ - passwordService   │
├─────────────────────┤
│ + registerUser()    │
│ + authenticateUser()│
│ + logoutUser()      │
│ + resetPassword()   │
└──────────┬──────────┘
           │ uses
           ▼
┌─────────────────────┐
│  UserRepository     │
├─────────────────────┤
│ - db                │
├─────────────────────┤
│ + findById()        │
│ + findByEmail()     │
│ + create()          │
│ + update()          │
│ + delete()          │
└─────────────────────┘
```

### 3.2 Class Specifications

#### 3.2.1 AuthController
**Purpose:** Handle HTTP requests for authentication endpoints

**Technology Used:**
- **Express.js** - Web framework for handling HTTP requests
  - *What it is:* Minimal Node.js framework for building APIs
  - *Why:* Simple routing, middleware support, widely used

**Attributes:**
- `authService: AuthService` - Instance of AuthService

**Methods:**

```typescript
class AuthController {
  private authService: AuthService;

  constructor(authService: AuthService) {
    this.authService = authService;
  }

  // Register new user
  async register(req: Request, res: Response): Promise<void> {
    const userData = req.body;
    const result = await this.authService.registerUser(userData);
    res.status(201).json(result);
  }

  // Login user
  async login(req: Request, res: Response): Promise<void> {
    const { email, password } = req.body;
    const result = await this.authService.authenticateUser(email, password);
    res.json(result);
  }

  // Logout user
  async logout(req: Request, res: Response): Promise<void> {
    const { userId, refreshToken } = req.body;
    await this.authService.logoutUser(userId, refreshToken);
    res.status(204).send();
  }
}
```

**Key Points:**
- Controller handles HTTP layer only
- Delegates business logic to AuthService
- Returns appropriate HTTP status codes
```

#### 3.2.2 AuthService
**Purpose:** Implement authentication business logic

**Technology Used:**
- **Dependency Injection** - Loose coupling between components
  - *What it is:* Pass dependencies through constructor
  - *Why:* Easy testing, flexible, maintainable

**Attributes:**
- `userRepository: IUserRepository` - Data access interface
- `tokenService: TokenService` - Token management
- `passwordService: PasswordService` - Password hashing/validation

**Methods:**

```typescript
class AuthService {
  constructor(
    private userRepository: IUserRepository,
    private tokenService: TokenService,
    private passwordService: PasswordService
  ) {}

  // Register new user
  async registerUser(userData: RegisterUserDTO): Promise<AuthResponse> {
    // 1. Validate email doesn't exist
    const existing = await this.userRepository.findByEmail(userData.email);
    if (existing) throw new ValidationError('Email already exists');

    // 2. Hash password
    const passwordHash = await this.passwordService.hash(userData.password);

    // 3. Create user
    const user = await this.userRepository.create({
      ...userData,
      passwordHash
    });

    // 4. Generate tokens
    const accessToken = this.tokenService.generateAccessToken(user);
    const refreshToken = this.tokenService.generateRefreshToken(user);

    return { user, accessToken, refreshToken };
  }

  // Authenticate user
  async authenticateUser(email: string, password: string): Promise<AuthResponse> {
    // 1. Find user
    const user = await this.userRepository.findByEmail(email);
    if (!user) throw new AuthenticationError('Invalid credentials');

    // 2. Verify password
    const valid = await this.passwordService.compare(password, user.passwordHash);
    if (!valid) throw new AuthenticationError('Invalid credentials');

    // 3. Generate tokens
    const accessToken = this.tokenService.generateAccessToken(user);
    const refreshToken = this.tokenService.generateRefreshToken(user);

    return { user, accessToken, refreshToken };
  }
}
```

**Key Points:**
- Contains business logic only
- Uses repository for data access
- Throws domain-specific errors

---

## 4. Data Models

### 4.1 Database Schema

#### 4.1.1 Users Table
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  role VARCHAR(50) DEFAULT 'user',
  is_active BOOLEAN DEFAULT true,
  email_verified BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);
```

#### 4.1.2 Refresh Tokens Table
```sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token VARCHAR(500) UNIQUE NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  revoked_at TIMESTAMP,
  is_revoked BOOLEAN DEFAULT false
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### 4.2 Entity Models

#### 4.2.1 User Entity
```typescript
interface User {
  id: string;
  email: string;
  passwordHash: string;
  firstName?: string;
  lastName?: string;
  role: UserRole;
  isActive: boolean;
  emailVerified: boolean;
  createdAt: Date;
  updatedAt: Date;
  lastLoginAt?: Date;
}

enum UserRole {
  ADMIN = 'admin',
  USER = 'user',
  GUEST = 'guest'
}
```

### 4.3 Data Transfer Objects (DTOs)

```typescript
// Registration DTO
interface RegisterUserDTO {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

// Login DTO
interface LoginDTO {
  email: string;
  password: string;
}

// Auth Response DTO
interface AuthResponse {
  user: {
    id: string;
    email: string;
    firstName?: string;
    lastName?: string;
    role: UserRole;
  };
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}
```

---

## 5. API Specifications

### 5.1 Register User

**Endpoint:** `POST /api/v1/auth/register`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules:**
- `email`: Required, valid email format, max 255 characters
- `password`: Required, min 8 characters, must contain uppercase, lowercase, number, special character
- `firstName`: Optional, max 100 characters
- `lastName`: Optional, max 100 characters

**Success Response (201 Created):**
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "user"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

**Error Responses:**

```json
// 400 Bad Request - Validation Error
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid input data",
  "details": [
    {
      "field": "email",
      "message": "Email is already registered"
    }
  ]
}

// 400 Bad Request - Weak Password
{
  "error": "WEAK_PASSWORD",
  "message": "Password does not meet security requirements"
}
```

### 5.2 Login

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "user"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

**Error Responses:**
```json
// 401 Unauthorized
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
}

// 403 Forbidden
{
  "error": "ACCOUNT_DISABLED",
  "message": "Your account has been disabled"
}
```

---

## 6. Algorithms and Logic

### 6.1 User Registration Flow

**Pseudocode:**
```
FUNCTION registerUser(userData):
  1. Validate input data
     - Check email format
     - Check password strength
     - Sanitize inputs
  
  2. Check if email already exists
     IF email exists THEN
       THROW ValidationError("Email already registered")
     END IF
  
  3. Hash password
     passwordHash = bcrypt.hash(userData.password, saltRounds=10)
  
  4. Create user record
     user = {
       email: userData.email,
       passwordHash: passwordHash,
       firstName: userData.firstName,
       lastName: userData.lastName,
       role: 'user',
       isActive: true,
       emailVerified: false
     }
  
  5. Save user to database
     savedUser = userRepository.create(user)
  
  6. Generate JWT tokens
     accessToken = tokenService.generateAccessToken(savedUser)
     refreshToken = tokenService.generateRefreshToken(savedUser)
  
  7. Save refresh token to database
     tokenRepository.saveRefreshToken(refreshToken, savedUser.id)
  
  8. Send verification email (async)
     emailService.sendVerificationEmail(savedUser.email)
  
  9. Return auth response
     RETURN {
       user: sanitizeUser(savedUser),
       accessToken: accessToken,
       refreshToken: refreshToken,
       expiresIn: 3600
     }
END FUNCTION
```

**Complexity:** O(1) - Constant time operations (database lookups are indexed)

### 6.2 User Authentication Flow

**Flowchart:**
```
START
  │
  ▼
[Receive email & password]
  │
  ▼
[Find user by email]
  │
  ├─[Not found]──► [Return 401 Error]
  │
  ▼
[User found]
  │
  ▼
[Check if account is active]
  │
  ├─[Inactive]──► [Return 403 Error]
  │
  ▼
[Compare password hash]
  │
  ├─[Invalid]──► [Return 401 Error]
  │
  ▼
[Password valid]
  │
  ▼
[Generate access token]
  │
  ▼
[Generate refresh token]
  │
  ▼
[Save refresh token]
  │
  ▼
[Update last login time]
  │
  ▼
[Return tokens & user data]
  │
  ▼
END
```

---

## 7. Interfaces and Contracts

### 7.1 Repository Interface

```typescript
interface IUserRepository {
  /**
   * Find user by ID
   * @param id - User ID
   * @returns User or null if not found
   */
  findById(id: string): Promise<User | null>;

  /**
   * Find user by email
   * @param email - User email
   * @returns User or null if not found
   */
  findByEmail(email: string): Promise<User | null>;

  /**
   * Create new user
   * @param user - User data
   * @returns Created user
   */
  create(user: Omit<User, 'id' | 'createdAt' | 'updatedAt'>): Promise<User>;

  /**
   * Update user
   * @param id - User ID
   * @param updates - Fields to update
   * @returns Updated user
   */
  update(id: string, updates: Partial<User>): Promise<User>;

  /**
   * Delete user
   * @param id - User ID
   */
  delete(id: string): Promise<void>;
}
```

---

## 8. Error Handling

### 8.1 Error Hierarchy

```typescript
class ApplicationError extends Error {
  constructor(
    public message: string,
    public statusCode: number,
    public errorCode: string
  ) {
    super(message);
    this.name = this.constructor.name;
  }
}

class ValidationError extends ApplicationError {
  constructor(message: string, public details?: any[]) {
    super(message, 400, 'VALIDATION_ERROR');
  }
}

class AuthenticationError extends ApplicationError {
  constructor(message: string = 'Invalid credentials') {
    super(message, 401, 'AUTHENTICATION_ERROR');
  }
}

class AuthorizationError extends ApplicationError {
  constructor(message: string = 'Access denied') {
    super(message, 403, 'AUTHORIZATION_ERROR');
  }
}

class NotFoundError extends ApplicationError {
  constructor(resource: string) {
    super(`${resource} not found`, 404, 'NOT_FOUND');
  }
}
```

### 8.2 Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| VALIDATION_ERROR | 400 | Invalid input data |
| WEAK_PASSWORD | 400 | Password doesn't meet requirements |
| INVALID_CREDENTIALS | 401 | Wrong email or password |
| TOKEN_EXPIRED | 401 | JWT token has expired |
| INVALID_TOKEN | 401 | JWT token is invalid |
| ACCOUNT_DISABLED | 403 | User account is disabled |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |
| USER_NOT_FOUND | 404 | User doesn't exist |
| INTERNAL_ERROR | 500 | Server error |

---

## 9. Security Implementation

### 9.1 Password Hashing

**Technology Used:**
- **bcrypt** - Password hashing algorithm
  - *What it is:* Secure one-way hashing function with salt
  - *Why:* Industry standard, slow by design (prevents brute force)
  - *Alternative:* Argon2 (newer, more secure)

**Implementation:**
```typescript
class PasswordService {
  private readonly SALT_ROUNDS = 10; // Higher = more secure but slower

  // Hash password before storing
  async hash(password: string): Promise<string> {
    return bcrypt.hash(password, this.SALT_ROUNDS);
  }

  // Compare plain password with hash
  async compare(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }

  // Validate password strength
  validate(password: string): boolean {
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
    return regex.test(password);
  }
}
```

**Password Policy:**
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- At least 1 special character

---

### 9.2 JWT Token Generation

**Technology Used:**
- **JWT (JSON Web Token)** - Stateless authentication
  - *What it is:* Encoded token containing user info and signature
  - *Why:* Stateless, scalable, works across services
  - *Library:* jsonwebtoken (npm package)

**Implementation:**
```typescript
class TokenService {
  private readonly ACCESS_TOKEN_EXPIRY = '15m';  // Short-lived
  private readonly REFRESH_TOKEN_EXPIRY = '7d';  // Long-lived

  // Generate access token (short-lived)
  generateAccessToken(user: User): string {
    return jwt.sign(
      { userId: user.id, email: user.email, role: user.role },
      process.env.ACCESS_TOKEN_SECRET,
      { expiresIn: this.ACCESS_TOKEN_EXPIRY }
    );
  }

  // Generate refresh token (long-lived)
  generateRefreshToken(user: User): string {
    return jwt.sign(
      { userId: user.id },
      process.env.REFRESH_TOKEN_SECRET,
      { expiresIn: this.REFRESH_TOKEN_EXPIRY }
    );
  }

  // Verify token
  verifyAccessToken(token: string): any {
    return jwt.verify(token, process.env.ACCESS_TOKEN_SECRET);
  }
}
```

**Token Strategy:**
- **Access Token:** 15 minutes (used for API requests)
- **Refresh Token:** 7 days (used to get new access token)
- **Storage:** Access token in memory, Refresh token in HTTP-only cookie

---

## 10. Testing Strategy

### 10.1 Unit Tests

**Test Cases for AuthService.registerUser():**

| Test Case | Input | Expected Output |
|-----------|-------|-----------------|
| Valid registration | Valid email, strong password | User created, tokens returned |
| Duplicate email | Existing email | ValidationError thrown |
| Weak password | Password without special char | ValidationError thrown |
| Invalid email format | "notanemail" | ValidationError thrown |
| Missing required fields | No email | ValidationError thrown |

**Example Test:**
```typescript
describe('AuthService.registerUser', () => {
  it('should register user with valid data', async () => {
    const userData = {
      email: 'test@example.com',
      password: 'SecurePass123!',
      firstName: 'John',
      lastName: 'Doe'
    };

    const result = await authService.registerUser(userData);

    expect(result.user.email).toBe(userData.email);
    expect(result.accessToken).toBeDefined();
    expect(result.refreshToken).toBeDefined();
  });

  it('should throw error for duplicate email', async () => {
    // Setup: Create user first
    await authService.registerUser({
      email: 'duplicate@example.com',
      password: 'SecurePass123!'
    });

    // Test: Try to register again
    await expect(
      authService.registerUser({
        email: 'duplicate@example.com',
        password: 'AnotherPass123!'
      })
    ).rejects.toThrow(ValidationError);
  });
});
```

### 10.2 Integration Tests

**Test Scenarios:**
1. Complete registration flow (API → Service → Repository → Database)
2. Login flow with valid credentials
3. Token refresh flow
4. Logout and token invalidation

### 10.3 Code Coverage Target
- **Unit Tests:** 90% coverage
- **Integration Tests:** 80% coverage
- **Critical Paths:** 100% coverage

---

## 11. Logging and Monitoring

### 11.1 Logging Strategy

```typescript
// Log levels: DEBUG, INFO, WARN, ERROR

// INFO: Successful operations
logger.info('User registered successfully', {
  userId: user.id,
  email: user.email,
  timestamp: new Date()
});

// WARN: Suspicious activity
logger.warn('Failed login attempt', {
  email: email,
  ipAddress: req.ip,
  timestamp: new Date()
});

// ERROR: Exceptions
logger.error('Database connection failed', {
  error: error.message,
  stack: error.stack,
  timestamp: new Date()
});
```

### 11.2 Metrics to Track
- Registration rate (per hour/day)
- Login success/failure rate
- Token refresh rate
- Average response time
- Error rate by type

---

## 12. Configuration

### 12.1 Environment Variables

```env
# Server
PORT=3000
NODE_ENV=development

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=auth_db
DB_USER=postgres
DB_PASSWORD=secret

# JWT
ACCESS_TOKEN_SECRET=your-access-token-secret
REFRESH_TOKEN_SECRET=your-refresh-token-secret
ACCESS_TOKEN_EXPIRY=15m
REFRESH_TOKEN_EXPIRY=7d

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Email
EMAIL_SERVICE=sendgrid
EMAIL_API_KEY=your-sendgrid-api-key
EMAIL_FROM=noreply@example.com
```

---

## 13. Deployment Considerations

### 13.1 Dependencies
```json
{
  "dependencies": {
    "express": "^4.18.2",
    "bcryptjs": "^2.4.3",
    "jsonwebtoken": "^9.0.0",
    "pg": "^8.11.0",
    "redis": "^4.6.5",
    "joi": "^17.9.1",
    "winston": "^3.8.2"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "jest": "^29.5.0",
    "@types/express": "^4.17.17",
    "@types/bcryptjs": "^2.4.2",
    "@types/jsonwebtoken": "^9.0.2"
  }
}
```

### 13.2 Docker Configuration
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["node", "dist/app.js"]
```

---

## 14. Future Enhancements

1. **Multi-factor Authentication (MFA):** Add TOTP-based 2FA
2. **Social Login:** OAuth integration (Google, Facebook, GitHub)
3. **Password History:** Prevent password reuse
4. **Account Lockout:** Lock account after N failed attempts
5. **Session Management:** View and revoke active sessions

---

## 15. Approval

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Developer | | | |
| Tech Lead | | | |
| Code Reviewer | | | |

