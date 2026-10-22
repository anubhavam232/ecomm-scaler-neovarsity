# E-Commerce Microservices Platform (Java 17 & Spring Boot 3.2)

Complete multi-module **Spring Boot 3.2.5 (Java 17)** microservices project implementing every functional and architectural requirement from `PRD_ HLD of Ecommerce Website.docx`.

## 1. Architecture & Microservices Overview

| Module / Component | Port | Primary Datastore & Tech Stack | PRD & HLD Responsibilities |
| :--- | :--- | :--- | :--- |
| **`api-gateway`** + **`infra/kong/kong.yml`** | `8080` / `8000` | Kong 3.6 + Spring Cloud Gateway + JWT + Rate Limiter | **HLD 1 & 2**: Entry point behind AWS ELB (`infra/aws-elb-cloudformation.yml`), request routing, token-bucket rate limiting (`120 req/min`), and HMAC-SHA256 Bearer JWT authentication. |
| **`user-service`** | `8081` | MySQL (`ecommerce_user_db`) + Kafka Producer (`user-events`) | **PRD 1.1–1.4, 6.1–6.2 & HLD 3.1**: Email & OAuth2 Social Login (`GOOGLE`, `FACEBOOK`, `GITHUB`), SHA-256 salted password hashing, JWT session management, profile management, and secure password reset links. |
| **`product-service`** | `8082` | MySQL (`ecommerce_product_db`) + Elasticsearch (`products_idx`) | **PRD 2.1–2.3 & HLD 3.2, 7 (Part 1 Flow)**: Category browsing, rich product details (images, specifications map), and Elasticsearch full-text search with Levenshtein fuzzy typo correction. |
| **`cart-service`** | `8083` | MongoDB (`ecommerce_cart_db`) + Redis Cache + Kafka Producer (`cart-events`) | **PRD 3.1–3.2 & HLD 3.3, 6 (Part 2 Flow)**: Flexible MongoDB cart documents, write-through/read-aside Redis caching (`ecommerce:cart:user:{id}`), automatic tax/total calculation, and `CartUpdatedEvent` publishing. |
| **`order-service`** | `8084` | MySQL (`ecommerce_order_db`) + Kafka Producer/Consumer (`order-events`, `payment-events`) | **PRD 3.3, 4.1–4.3 & HLD 3.4 (Part 3 Flow)**: Seamless checkout, asynchronous payment verification via Kafka, order confirmation, user order history, and real-time delivery tracking checkpoints. |
| **`payment-service`** | `8085` | MySQL (`ecommerce_payment_db`) + Kafka Consumer/Producer (`order-events`, `payment-events`) | **PRD 5.1–5.3 & HLD 3.5 (Part 3 Flow)**: Multi-option payment processing (`CREDIT_CARD`, `DEBIT_CARD`, `ONLINE_BANKING`, `UPI`, `WALLET`), PCI-DSS card masking, cryptographic signature verification, and digital receipts. |
| **`notification-service`** | `8086` | Kafka Consumer (`user-events`, `order-events`, `payment-events`) + Amazon SES | **HLD 3.6**: Consumes registration, password reset, payment receipt, and order tracking events from Kafka and dispatches transactional emails via Amazon SES (`SesClient`). |
| **`common-dto`** | N/A | Shared Library | Shared `ApiResponse<T>`, Kafka topic & event record definitions, and RFC 7519 `JwtTokenProvider`. |

## 2. Running the Project

### Full Docker Compose Stack (MySQL, MongoDB, Redis, Kafka, Elasticsearch, Kong)

```bash
# Build all Spring Boot Maven modules and launch the complete infrastructure + microservices stack:
docker-compose up --build -d

# Check container status:
docker-compose ps
```

## 3. REST API Reference (via API Gateway `http://localhost:8080`)

### User Management & Authentication (`user-service` - Port `8081`)
- `POST /api/v1/users/register` — Register new account (publishes `UserRegisteredEvent` to `user-events`)
- `POST /api/v1/users/oauth2/social-login` — Register/Login via Social Media (`GOOGLE`, `FACEBOOK`, `GITHUB`)
- `POST /api/v1/auth/login` — Authenticate and receive Bearer JWT + session ID
- `GET /api/v1/auth/session` — Validate active JWT session (`Authorization: Bearer <token>`)
- `POST /api/v1/auth/logout` — Revoke JWT token and terminate session
- `GET /api/v1/users/{userId}/profile` — View user profile
- `PUT /api/v1/users/{userId}/profile` — Update full name, phone, and default shipping address
- `POST /api/v1/users/password-reset/request` — Generate time-limited reset token & email secure link via Kafka
- `POST /api/v1/users/password-reset/confirm` — Reset password using secure token

### Product Catalog & Elasticsearch Search (`product-service` - Port `8082`)
- `GET /api/v1/products?category={category}` — Browse products (all or filtered by category)
- `GET /api/v1/products/categories` — List all categories with product counts
- `GET /api/v1/products/{productId}` — Detailed product view (images, description, specifications map)
- `POST /api/v1/products` — Create product in MySQL and index in Elasticsearch (`products_idx`)
- `GET /api/v1/products/search?q={keyword}&category={cat}&minPrice={min}&maxPrice={max}` — Full-text Elasticsearch search with fuzzy typo correction

### Shopping Cart (`cart-service` - Port `8083`)
- `POST /api/v1/cart/{userId}/items` — Add product to cart (writes to MongoDB + Redis cache + publishes `CartUpdatedEvent` to `cart-events`)
- `GET /api/v1/cart/{userId}` — Review cart items, quantities, tax, and grand total from Redis cache (`REDIS_CACHE_HIT`)
- `PUT /api/v1/cart/{userId}/items/{productId}` — Update item quantity
- `DELETE /api/v1/cart/{userId}/items/{productId}` — Remove item from cart
- `DELETE /api/v1/cart/{userId}` — Clear cart

### Order Management & Checkout (`order-service` - Port `8084`)
- `POST /api/v1/orders/checkout` — Create order in MySQL & publish `OrderPlacedEvent` to `order-events` for asynchronous payment processing
- `GET /api/v1/orders/{orderId}` — View Order Confirmation & payment verification details
- `GET /api/v1/orders/user/{userId}` — View complete Order History for a user
- `GET /api/v1/orders/{orderId}/tracking` — Track order delivery status and checkpoint timeline
- `PUT /api/v1/orders/{orderId}/tracking` — Advance delivery tracking status (publishes `OrderStatusUpdatedEvent`)

### Payment & Receipts (`payment-service` - Port `8085`)
- `GET /api/v1/payments/methods` — List supported payment methods (`CREDIT_CARD`, `DEBIT_CARD`, `ONLINE_BANKING`, `UPI`, `WALLET`)
- `POST /api/v1/payments/process` — Process direct payment transaction
- `GET /api/v1/payments/receipt/order/{orderId}` — Retrieve itemized digital payment receipt with PCI-masked instrument & HMAC signature

### Notifications (`notification-service` - Port `8086`)
- `GET /api/v1/notifications` — List all Amazon SES notifications dispatched from Kafka events
- `GET /api/v1/notifications/user/{userId}` — List notifications sent to a specific user
