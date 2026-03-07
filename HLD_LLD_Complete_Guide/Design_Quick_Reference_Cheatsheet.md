# Design Quick Reference Cheatsheet

## 📋 HLD Checklist

### ✅ Must-Have Components
- [ ] Executive Summary
- [ ] Project Scope (In/Out of scope)
- [ ] Requirements Summary (Functional & Non-functional)
- [ ] System Architecture Diagram
- [ ] Component Diagram
- [ ] Technology Stack
- [ ] Data Architecture
- [ ] API Strategy
- [ ] Security Approach
- [ ] Scalability Plan
- [ ] Deployment Architecture
- [ ] Risk Assessment
- [ ] Timeline & Milestones

### 📊 Key Diagrams
- [ ] System Context Diagram
- [ ] Architecture Diagram
- [ ] Component Diagram
- [ ] Deployment Diagram
- [ ] Data Flow Diagram
- [ ] Network Diagram

### 🎯 Questions HLD Should Answer
1. What is the overall system architecture?
2. What are the major components?
3. How do components communicate?
4. What technologies will be used?
5. How will the system scale?
6. How will the system be deployed?
7. What are the security measures?
8. What are the risks and mitigations?

---

## 📋 LLD Checklist

### ✅ Must-Have Components
- [ ] Module/Package Structure
- [ ] Class Diagrams
- [ ] Detailed Database Schema
- [ ] Complete API Specifications
- [ ] Sequence Diagrams
- [ ] Algorithm Pseudocode
- [ ] Data Structures
- [ ] Error Handling Strategy
- [ ] Interface Definitions
- [ ] Testing Strategy
- [ ] Logging Specification
- [ ] Configuration Management

### 📊 Key Diagrams
- [ ] Class Diagram
- [ ] Sequence Diagram
- [ ] Activity Diagram
- [ ] State Diagram
- [ ] ER Diagram (detailed)
- [ ] Flowcharts

### 🎯 Questions LLD Should Answer
1. What classes/modules are needed?
2. What are the class attributes and methods?
3. What is the exact database schema?
4. What are the API request/response formats?
5. What algorithms will be used?
6. How will errors be handled?
7. What are the validation rules?
8. How will the code be tested?

---

## 🔄 Design Process Flow

```
1. Gather Requirements
   ↓
2. Create HLD
   ↓
3. Review HLD with Stakeholders
   ↓
4. Get HLD Approval
   ↓
5. Create LLD (per component)
   ↓
6. Review LLD with Team
   ↓
7. Get LLD Approval
   ↓
8. Start Implementation
   ↓
9. Update Designs as Needed
```

---

## 🎨 Architecture Patterns Quick Reference

### Monolithic
**When:** Small to medium applications, simple requirements  
**Pros:** Simple to develop, deploy, test  
**Cons:** Hard to scale, tight coupling

### Microservices
**When:** Large, complex systems, need independent scaling  
**Pros:** Independent deployment, technology flexibility  
**Cons:** Complex infrastructure, distributed system challenges

### Serverless
**When:** Event-driven, variable load, cost optimization  
**Pros:** No server management, auto-scaling, pay-per-use  
**Cons:** Cold starts, vendor lock-in, debugging challenges

### Event-Driven
**When:** Real-time processing, async workflows  
**Pros:** Loose coupling, scalability, resilience  
**Cons:** Complexity, eventual consistency

### Layered (N-Tier)
**When:** Traditional enterprise applications  
**Pros:** Separation of concerns, maintainability  
**Cons:** Can become monolithic, performance overhead

---

## 🗄️ Database Selection Guide

| Database Type | Use Case | Examples |
|---------------|----------|----------|
| **Relational (SQL)** | Structured data, ACID transactions, complex queries | PostgreSQL, MySQL, Oracle |
| **Document (NoSQL)** | Flexible schema, JSON-like data | MongoDB, CouchDB |
| **Key-Value** | Caching, session storage, simple lookups | Redis, Memcached |
| **Column-Family** | Time-series, analytics, big data | Cassandra, HBase |
| **Graph** | Relationships, social networks, recommendations | Neo4j, Amazon Neptune |
| **Time-Series** | Metrics, IoT, monitoring data | InfluxDB, TimescaleDB |

---

## 🔐 Security Checklist

### Authentication
- [ ] Password hashing (bcrypt, Argon2)
- [ ] JWT token implementation
- [ ] Token expiry and refresh
- [ ] Multi-factor authentication (if needed)
- [ ] OAuth/SSO integration (if needed)

### Authorization
- [ ] Role-based access control (RBAC)
- [ ] Permission management
- [ ] API authorization checks

### Data Protection
- [ ] TLS/SSL for data in transit
- [ ] Encryption for data at rest
- [ ] Sensitive data masking in logs
- [ ] Secure credential storage

### API Security
- [ ] Input validation
- [ ] SQL injection prevention
- [ ] XSS prevention
- [ ] CSRF protection
- [ ] Rate limiting
- [ ] CORS configuration

### Compliance
- [ ] GDPR compliance (if applicable)
- [ ] HIPAA compliance (if applicable)
- [ ] Data retention policies
- [ ] Audit logging

---

## 📈 Scalability Patterns

### Horizontal Scaling
- Add more servers/instances
- Load balancing required
- Stateless services preferred

### Vertical Scaling
- Increase server resources (CPU, RAM)
- Simpler but has limits
- Downtime during scaling

### Caching Strategies
- **Cache-Aside:** App checks cache, then DB
- **Write-Through:** Write to cache and DB simultaneously
- **Write-Behind:** Write to cache, async to DB
- **Refresh-Ahead:** Proactively refresh cache

### Database Scaling
- **Read Replicas:** Scale read operations
- **Sharding:** Partition data across databases
- **Partitioning:** Split tables horizontally/vertically
- **Connection Pooling:** Reuse database connections

---

## 🧪 Testing Strategy

### Unit Tests
- Test individual functions/methods
- Mock dependencies
- Target: 80-90% code coverage

### Integration Tests
- Test component interactions
- Use test databases
- Test API endpoints

### End-to-End Tests
- Test complete user flows
- Simulate real user scenarios
- Use staging environment

### Performance Tests
- Load testing (expected load)
- Stress testing (beyond capacity)
- Spike testing (sudden load increase)
- Endurance testing (sustained load)

---

## 📊 Non-Functional Requirements Template

### Performance
- Response time: < X ms (p95)
- Throughput: X requests/second
- Page load time: < X seconds

### Scalability
- Concurrent users: X
- Data volume: X GB/TB
- Growth rate: X% per year

### Availability
- Uptime: 99.X%
- RTO (Recovery Time Objective): X hours
- RPO (Recovery Point Objective): X hours

### Security
- Authentication method
- Authorization model
- Encryption standards
- Compliance requirements

### Maintainability
- Code coverage: X%
- Documentation standards
- Code review process

---

## 🛠️ SOLID Principles Quick Reference

**S - Single Responsibility**  
A class should have one, and only one, reason to change.

**O - Open/Closed**  
Open for extension, closed for modification.

**L - Liskov Substitution**  
Derived classes must be substitutable for their base classes.

**I - Interface Segregation**  
Many specific interfaces are better than one general interface.

**D - Dependency Inversion**  
Depend on abstractions, not concretions.

---

## 🎯 Design Patterns Quick Reference

### Creational Patterns
- **Singleton:** Single instance of a class
- **Factory:** Create objects without specifying exact class
- **Builder:** Construct complex objects step by step
- **Prototype:** Clone existing objects

### Structural Patterns
- **Adapter:** Make incompatible interfaces work together
- **Decorator:** Add behavior to objects dynamically
- **Facade:** Simplified interface to complex subsystem
- **Proxy:** Placeholder for another object

### Behavioral Patterns
- **Strategy:** Select algorithm at runtime
- **Observer:** Notify multiple objects of state changes
- **Command:** Encapsulate requests as objects
- **State:** Change behavior when state changes
- **Template Method:** Define skeleton, let subclasses override steps

---

## 📝 API Design Best Practices

### RESTful API Guidelines
- Use nouns for resources: `/users`, `/products`
- Use HTTP methods correctly:
  - GET: Retrieve
  - POST: Create
  - PUT: Update (full)
  - PATCH: Update (partial)
  - DELETE: Remove
- Use proper status codes:
  - 200: OK
  - 201: Created
  - 400: Bad Request
  - 401: Unauthorized
  - 403: Forbidden
  - 404: Not Found
  - 500: Internal Server Error
- Version your APIs: `/api/v1/users`
- Use pagination for lists
- Provide filtering and sorting
- Return consistent error format

### API Documentation
- Use OpenAPI/Swagger
- Provide examples
- Document authentication
- List all error codes
- Include rate limits

---

## 🔍 Code Review Checklist

### Functionality
- [ ] Code works as expected
- [ ] Edge cases handled
- [ ] Error handling implemented

### Design
- [ ] Follows SOLID principles
- [ ] Appropriate design patterns used
- [ ] No code duplication (DRY)
- [ ] Proper separation of concerns

### Code Quality
- [ ] Readable and maintainable
- [ ] Follows naming conventions
- [ ] Properly commented
- [ ] No magic numbers/strings

### Testing
- [ ] Unit tests written
- [ ] Tests pass
- [ ] Good test coverage

### Security
- [ ] Input validation
- [ ] No hardcoded secrets
- [ ] SQL injection prevention
- [ ] XSS prevention

### Performance
- [ ] No obvious performance issues
- [ ] Efficient algorithms
- [ ] Proper indexing (database)
- [ ] Caching where appropriate

---

## 📚 Documentation Standards

### Code Comments
```typescript
/**
 * Register a new user in the system
 * 
 * @param userData - User registration data
 * @returns Created user with tokens
 * @throws ValidationError if email already exists
 * @throws ValidationError if password is weak
 * 
 * @example
 * const result = await registerUser({
 *   email: 'user@example.com',
 *   password: 'SecurePass123!'
 * });
 */
async registerUser(userData: RegisterUserDTO): Promise<AuthResponse>
```

### README Template
```markdown
# Project Name

## Description
Brief description of what the project does

## Prerequisites
- Node.js 18+
- PostgreSQL 15+
- Redis 7+

## Installation
npm install

## Configuration
Copy .env.example to .env and configure

## Running
npm run dev

## Testing
npm test

## Deployment
Instructions for deployment

## API Documentation
Link to API docs

## Contributing
Guidelines for contributors

## License
License information
```

---

## 🚀 Deployment Checklist

### Pre-Deployment
- [ ] All tests passing
- [ ] Code reviewed and approved
- [ ] Security scan completed
- [ ] Performance testing done
- [ ] Documentation updated
- [ ] Database migrations ready
- [ ] Environment variables configured
- [ ] Monitoring setup
- [ ] Logging configured
- [ ] Backup strategy in place

### Deployment
- [ ] Deploy to staging first
- [ ] Smoke tests on staging
- [ ] Get approval for production
- [ ] Deploy to production
- [ ] Run smoke tests
- [ ] Monitor for errors
- [ ] Verify metrics

### Post-Deployment
- [ ] Monitor application health
- [ ] Check error rates
- [ ] Verify performance metrics
- [ ] Update status page
- [ ] Notify stakeholders
- [ ] Document any issues

---

## 🎓 Learning Resources

### Books
- "Designing Data-Intensive Applications" by Martin Kleppmann
- "Clean Architecture" by Robert C. Martin
- "System Design Interview" by Alex Xu
- "Domain-Driven Design" by Eric Evans

### Online Resources
- System Design Primer (GitHub)
- AWS Architecture Center
- Microsoft Azure Architecture Center
- Google Cloud Architecture Framework

### Tools to Learn
- Draw.io / Lucidchart (Diagrams)
- PlantUML (UML diagrams)
- Swagger/OpenAPI (API docs)
- Docker & Kubernetes (Deployment)

---

## 💡 Pro Tips

1. **Start Simple:** Don't over-engineer, start with simplest solution
2. **Iterate:** Design is iterative, expect changes
3. **Document Decisions:** Explain WHY, not just WHAT
4. **Get Feedback Early:** Review designs before coding
5. **Think About Failure:** Plan for errors, outages, edge cases
6. **Consider Operations:** Think about monitoring, debugging, maintenance
7. **Security First:** Don't add security as afterthought
8. **Performance Matters:** Consider performance from the start
9. **Keep It Updated:** Update designs as system evolves
10. **Learn from Others:** Study existing system designs

---

## 📞 When to Ask for Help

- Unclear requirements
- Technology choice uncertainty
- Scalability concerns
- Security requirements
- Complex algorithms
- Performance bottlenecks
- Integration challenges
- Compliance requirements

**Remember:** It's better to ask early than to redesign later!

---

**Happy Designing! 🎨**

