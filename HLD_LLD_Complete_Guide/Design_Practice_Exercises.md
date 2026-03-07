# Design Practice Exercises & Real-World Examples

## 🎯 Purpose
This document provides hands-on exercises to practice creating HLD and LLD documents. Work through these exercises to build your design skills.

---

## Exercise 1: URL Shortener (Like bit.ly)

### 📝 Requirements
Create a URL shortening service where:
- Users can submit a long URL and get a short URL
- When someone visits the short URL, they're redirected to the original URL
- Track click statistics (number of clicks per URL)
- Support custom short URLs (optional)
- Handle 1 million URLs and 10 million clicks per day

### Your Task: Create HLD
**What to include:**
1. System architecture diagram
2. Component identification (API, Database, Cache, etc.)
3. Technology stack selection
4. Database design (high-level)
5. API endpoints (high-level)
6. Scalability approach
7. How to generate short URLs (algorithm choice)

### Your Task: Create LLD (for URL Creation Service)
**What to include:**
1. Class diagram
2. Detailed database schema
3. API specification with request/response
4. Short URL generation algorithm (pseudocode)
5. Sequence diagram for URL creation
6. Error handling
7. Validation rules

### 💡 Hints
- Consider using base62 encoding for short URLs
- Think about collision handling
- Consider caching frequently accessed URLs
- Think about database indexing
- Consider rate limiting to prevent abuse

---

## Exercise 2: Task Management System (Like Trello)

### 📝 Requirements
Create a task management system where:
- Users can create boards, lists, and cards
- Users can drag and drop cards between lists
- Support for task assignments
- Support for comments on cards
- Real-time updates when team members make changes
- Support for file attachments
- Search functionality

### Your Task: Create HLD
**What to include:**
1. System architecture (consider real-time requirements)
2. Component breakdown
3. Technology stack (consider WebSocket for real-time)
4. Database design approach
5. File storage strategy
6. Real-time update mechanism
7. Search implementation approach

### Your Task: Create LLD (for Card Management Service)
**What to include:**
1. Class diagram for Card, List, Board
2. Database schema with relationships
3. API endpoints for CRUD operations
4. WebSocket event specifications
5. Sequence diagram for moving a card
6. File upload handling
7. Search indexing strategy

### 💡 Hints
- Consider using WebSocket or Server-Sent Events for real-time
- Think about optimistic UI updates
- Consider using Elasticsearch for search
- Think about file size limits and validation
- Consider using S3 or similar for file storage

---

## Exercise 3: E-commerce Shopping Cart

### 📝 Requirements
Create a shopping cart system where:
- Users can add/remove items from cart
- Cart persists across sessions
- Calculate total with taxes and discounts
- Support for promo codes
- Inventory management (prevent overselling)
- Guest checkout support
- Cart abandonment tracking

### Your Task: Create HLD
**What to include:**
1. System architecture
2. Component identification
3. Database strategy (consider cart persistence)
4. Session management approach
5. Inventory management strategy
6. Integration with payment gateway
7. Caching strategy

### Your Task: Create LLD (for Cart Service)
**What to include:**
1. Class diagram
2. Database schema (cart, cart_items, products)
3. API specifications
4. Cart calculation algorithm (with taxes, discounts)
5. Inventory locking mechanism
6. Promo code validation logic
7. Error handling for out-of-stock scenarios

### 💡 Hints
- Consider using Redis for cart storage
- Think about race conditions in inventory
- Consider using database transactions
- Think about cart expiration
- Consider guest vs authenticated user carts

---

## Exercise 4: Social Media Feed (Like Twitter/X)

### 📝 Requirements
Create a social media feed where:
- Users can post messages (max 280 characters)
- Users can follow other users
- Users see posts from people they follow
- Support for likes and comments
- Real-time feed updates
- Handle 100 million users
- Handle 500 million posts per day

### Your Task: Create HLD
**What to include:**
1. System architecture (consider scale)
2. Feed generation strategy (push vs pull)
3. Database design (consider sharding)
4. Caching strategy
5. Real-time update mechanism
6. CDN for media
7. Load balancing approach

### Your Task: Create LLD (for Feed Generation Service)
**What to include:**
1. Class diagram
2. Database schema (users, posts, follows, likes)
3. Feed generation algorithm
4. API specifications
5. Sequence diagram for posting
6. Caching strategy details
7. Pagination implementation

### 💡 Hints
- Consider fan-out on write vs fan-out on read
- Think about celebrity users (millions of followers)
- Consider using message queues
- Think about eventual consistency
- Consider using time-based pagination

---

## Exercise 5: Online Code Editor (Like CodePen)

### 📝 Requirements
Create an online code editor where:
- Users can write HTML, CSS, JavaScript
- Live preview of code
- Save and share projects
- Support for multiple files
- Syntax highlighting
- Auto-save functionality
- Collaboration (optional)

### Your Task: Create HLD
**What to include:**
1. System architecture
2. Frontend architecture
3. Code execution strategy (client vs server)
4. Storage strategy
5. Real-time preview mechanism
6. Sharing mechanism
7. Security considerations

### Your Task: Create LLD (for Code Execution Service)
**What to include:**
1. Class diagram
2. Database schema
3. API specifications
4. Code sanitization algorithm
5. Preview generation logic
6. Auto-save mechanism
7. Security measures (XSS prevention)

### 💡 Hints
- Consider running code in sandboxed iframe
- Think about XSS prevention
- Consider using localStorage for auto-save
- Think about code size limits
- Consider using Monaco Editor or CodeMirror

---

## Exercise 6: Video Streaming Platform (Like YouTube - Simplified)

### 📝 Requirements
Create a video streaming platform where:
- Users can upload videos
- Users can watch videos
- Support for different video qualities
- Video recommendations
- Comments and likes
- View count tracking
- Search functionality

### Your Task: Create HLD
**What to include:**
1. System architecture
2. Video upload and processing pipeline
3. Video storage strategy (CDN)
4. Streaming protocol selection
5. Recommendation engine approach
6. Search implementation
7. Scalability for millions of videos

### Your Task: Create LLD (for Video Upload Service)
**What to include:**
1. Class diagram
2. Database schema
3. Upload API specification
4. Video processing workflow
5. Transcoding algorithm/service
6. Thumbnail generation
7. Progress tracking mechanism

### 💡 Hints
- Consider using S3 or similar for storage
- Think about video transcoding (FFmpeg)
- Consider using HLS or DASH for streaming
- Think about CDN for video delivery
- Consider using Elasticsearch for search
- Think about async processing for uploads

---

## Exercise 7: Restaurant Reservation System

### 📝 Requirements
Create a restaurant reservation system where:
- Customers can search for restaurants
- Customers can view available time slots
- Customers can make reservations
- Restaurants can manage their availability
- Send confirmation emails/SMS
- Handle cancellations and modifications
- Prevent double-booking

### Your Task: Create HLD
**What to include:**
1. System architecture
2. Component breakdown
3. Database design approach
4. Search functionality
5. Notification system
6. Availability calculation strategy
7. Integration with external services (email, SMS)

### Your Task: Create LLD (for Reservation Service)
**What to include:**
1. Class diagram
2. Database schema (restaurants, tables, reservations)
3. API specifications
4. Availability calculation algorithm
5. Double-booking prevention mechanism
6. Sequence diagram for making reservation
7. Cancellation policy implementation

### 💡 Hints
- Consider using database transactions
- Think about time zones
- Consider using locks to prevent double-booking
- Think about reservation expiration
- Consider using message queue for notifications

---

## Exercise 8: Chat Application (Like WhatsApp - Simplified)

### 📝 Requirements
Create a chat application where:
- One-on-one messaging
- Group chats
- Real-time message delivery
- Message history
- Online/offline status
- Read receipts
- File sharing

### Your Task: Create HLD
**What to include:**
1. System architecture
2. Real-time communication protocol (WebSocket)
3. Message storage strategy
4. File storage approach
5. Presence system (online/offline)
6. Scalability approach
7. Message delivery guarantees

### Your Task: Create LLD (for Messaging Service)
**What to include:**
1. Class diagram
2. Database schema (users, conversations, messages)
3. WebSocket event specifications
4. Message delivery algorithm
5. Read receipt mechanism
6. File upload handling
7. Offline message queue

### 💡 Hints
- Consider using WebSocket for real-time
- Think about message ordering
- Consider using message queues
- Think about end-to-end encryption (optional)
- Consider using Redis for presence

---

## 🎓 Self-Assessment Checklist

After completing each exercise, check if your design includes:

### HLD Checklist
- [ ] Clear architecture diagram
- [ ] All major components identified
- [ ] Technology stack with rationale
- [ ] Database type selection
- [ ] API strategy defined
- [ ] Scalability approach
- [ ] Security considerations
- [ ] Non-functional requirements addressed

### LLD Checklist
- [ ] Detailed class diagram
- [ ] Complete database schema with indexes
- [ ] Full API specifications with examples
- [ ] Algorithms in pseudocode
- [ ] Sequence diagrams for main flows
- [ ] Error handling strategy
- [ ] Validation rules
- [ ] Testing approach

---

## 🏆 Advanced Challenges

Once you're comfortable with the basics, try these:

### Challenge 1: Design Instagram
- Photo/video sharing
- Feed algorithm
- Stories (24-hour expiry)
- Direct messaging
- Explore page
- Scale: 1 billion users

### Challenge 2: Design Uber
- Real-time location tracking
- Ride matching algorithm
- Pricing calculation
- Payment processing
- Driver and rider apps
- Scale: millions of rides per day

### Challenge 3: Design Netflix
- Video streaming
- Recommendation engine
- Content delivery network
- User profiles
- Download for offline viewing
- Scale: 200 million subscribers

### Challenge 4: Design Google Drive
- File storage and sync
- Real-time collaboration
- Version history
- Sharing and permissions
- Search functionality
- Scale: petabytes of data

---

## 📚 Learning Path

### Beginner
1. Start with Exercise 1 (URL Shortener)
2. Move to Exercise 3 (Shopping Cart)
3. Try Exercise 7 (Restaurant Reservation)

### Intermediate
1. Exercise 2 (Task Management)
2. Exercise 5 (Code Editor)
3. Exercise 8 (Chat Application)

### Advanced
1. Exercise 4 (Social Media Feed)
2. Exercise 6 (Video Streaming)
3. Advanced Challenges

---

## 💡 Tips for Success

1. **Start with Requirements:** Make sure you understand what you're building
2. **Think About Scale:** Consider how the system will handle growth
3. **Consider Trade-offs:** Every design decision has pros and cons
4. **Draw Diagrams:** Visual representations help clarify thinking
5. **Be Specific:** Especially in LLD, provide concrete details
6. **Think About Failures:** What happens when things go wrong?
7. **Review Real Systems:** Study how real companies solve these problems
8. **Get Feedback:** Share your designs and get input from others
9. **Iterate:** First design is rarely perfect, refine it
10. **Practice Regularly:** Design skills improve with practice

---

## 🔍 Where to Find Solutions

### Study Real System Designs
- **Engineering Blogs:**
  - Netflix Tech Blog
  - Uber Engineering
  - Airbnb Engineering
  - Facebook Engineering
  - Twitter Engineering

- **System Design Resources:**
  - System Design Primer (GitHub)
  - Grokking the System Design Interview
  - System Design Interview by Alex Xu
  - High Scalability blog

### Compare Your Designs
After completing an exercise:
1. Search for real-world implementations
2. Compare your approach with industry solutions
3. Identify what you missed
4. Learn from the differences
5. Refine your design

---

## 📝 Practice Template

For each exercise, create two documents:

### Document 1: HLD_[ProjectName].md
```markdown
# High-Level Design: [Project Name]

## 1. Requirements Summary
[List requirements]

## 2. System Architecture
[Architecture diagram and description]

## 3. Components
[List and describe components]

## 4. Technology Stack
[List technologies with rationale]

## 5. Database Design
[High-level database approach]

## 6. API Design
[High-level API strategy]

## 7. Scalability
[How system will scale]

## 8. Security
[Security measures]

## 9. Trade-offs
[Design decisions and trade-offs]
```

### Document 2: LLD_[ComponentName].md
```markdown
# Low-Level Design: [Component Name]

## 1. Class Diagram
[UML class diagram]

## 2. Database Schema
[Detailed schema with DDL]

## 3. API Specifications
[Complete API docs]

## 4. Algorithms
[Pseudocode for key algorithms]

## 5. Sequence Diagrams
[Interaction diagrams]

## 6. Error Handling
[Error types and handling]

## 7. Testing Strategy
[How to test this component]
```

---

## 🎯 Next Steps

1. **Choose an exercise** that matches your skill level
2. **Set a time limit** (2-4 hours for HLD, 2-4 hours for LLD)
3. **Create the designs** using the templates
4. **Review your work** against the checklist
5. **Research real implementations** and compare
6. **Refine your design** based on learnings
7. **Move to the next exercise**

---

**Remember:** The goal is not perfection, but learning and improvement. Every design you create makes you better!

**Happy Designing! 🚀**

