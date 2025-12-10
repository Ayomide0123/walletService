# **Wallet Service API**

A secure and modular wallet system built with **Spring Boot**, supporting:

* **JWT Authentication**
* **API Key Authentication**
* **Paystack Payments (Redirect + Webhook)**
* **Google OAuth2 Login**
* **Wallet Deposit + Transfer**
* **Transaction History**
* **Swagger API Documentation**
* **Public Home Landing Page**

This service powers deposit & transfer operations, automated payment verification via Paystack, user authentication via Google OAuth, and fine-grained access control using API keys.

---

## 📂 **Project Structure**

```
src/main/java/com/hng/walletService
│
├── config/                 # Security configuration
├── controller/             # REST controllers + HomeController
├── model/                  
│   ├── dto/                # Request/Response DTOs
│   ├── entity/             # JPA Entities (User, Wallet, Transaction, ApiKey)
├── repository/             # Spring Data JPA repositories
├── security/               # Filters for JWT + API Keys
├── service/                # Business logic (Wallet, Paystack, Transactions)
└── util/                   # Authentication utilities
```

---

# **Features Overview**

## Authentication

### **1. JWT Authentication**

Users authenticated via Google OAuth receive a JWT token for all subsequent API requests.

### **2. API Key Authentication**

Developers can create API keys with specific permissions:

* `deposit`
* `transfer`
* `read`

API keys are validated through a custom `ApiKeyAuthenticationFilter`.

---

## 👤 Google OAuth2 Login

* Entry point: `/auth/google`
* Callback: `/auth/google/callback`
* Successful login returns a **JWT token + user details**

---

## 💳 Paystack Payments

### **Deposit Flow**

1. `/wallet/deposit` → Initiates Paystack transaction
2. User completes payment on Paystack
3. Paystack redirects to your callback:

   ```
   /wallet/verify-payment?reference=xxxx
   ```
4. Server verifies transaction using:

   ```
   GET https://api.paystack.co/transaction/verify/{reference}
   ```
5. Transaction is completed and wallet balance is updated.

### **Webhook**

Paystack also notifies your backend via webhook:

```
POST /wallet/paystack/webhook
```

Webhook:

* Verifies signature (`HMAC SHA512`)
* Prevents double-processing
* Ensures deposit is updated even if redirect fails

---

## Wallet Operations

### `/wallet/deposit`

Initiates a Paystack payment request for a user.

### `/wallet/transfer`

Transfer funds between wallets after full validation.

### `/wallet/balance`

Fetch user’s current wallet balance.

### `/wallet/transactions`

Retrieve history of deposits, transfers, and system events.

---

# Public Home Page

Accessible at:

```
GET /
```

Serves `home.html` – a clean landing page with:

* App name + description
* CTA buttons for Swagger UI & OAuth login
* Documentation links

This route is **public**, even with Spring Security enabled.

---

# API Documentation

Swagger available at:

```
/swagger-ui/index.html
```

OpenAPI JSON:

```
/v3/api-docs
```

---

# **Installation & Setup**

## 1️. Clone Repository

```bash
git clone https://github.com/Ayomide0123/walletService
cd walletService
```

## 2️⃣ Set Up Environment Variables

Create `application.properties` or `application.yml`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/walletdb
spring.datasource.username=postgres
spring.datasource.password=yourpassword

paystack.secret.key=sk_live_xxxxxxxxxxxxx
paystack.public.key=pk_live_xxxxxxxxxxxxx
paystack.base.url=https://api.paystack.co
paystack.callback.url=https://yourdomain.com/wallet/verify-payment

spring.security.oauth2.client.registration.google.client-id=xxxx
spring.security.oauth2.client.registration.google.client-secret=xxxx
```

---
# Endpoints Overview

## Authentication

| Method | Endpoint                | Description             |
| ------ | ----------------------- | ----------------------- |
| GET    | `/auth/google`          | Begin OAuth login       |
| GET    | `/auth/google/callback` | OAuth callback with JWT |

---

## Wallet

| Method | Endpoint                             | Description                       |
| ------ | ------------------------------------ | --------------------------------- |
| POST   | `/wallet/deposit`                    | Start Paystack payment            |
| GET    | `/wallet/verify-payment`             | Redirect callback                 |
| POST   | `/wallet/paystack/webhook`           | Paystack server-side notification |
| GET    | `/wallet/balance`                    | Get wallet balance                |
| POST   | `/wallet/transfer`                   | Transfer funds                    |
| GET    | `/wallet/transactions`               | List all transactions             |
| GET    | `/wallet/deposit/{reference}/status` | Check deposit status              |

---

## API Keys

| Method | Endpoint            | Description            |
| ------ | ------------------- | ---------------------- |
| POST   | `/keys/create`      | Create API key         |
| POST   | `/keys/rollover`    | Replace an expired key |
| GET    | `/keys/list`        | List API keys          |
| DELETE | `/keys/{id}/revoke` | Revoke API key         |

---

# Technology Stack

* **Spring Boot 3**
* **Spring Security**
* **Spring OAuth2 Client**
* **Spring WebFlux WebClient**
* **PostgreSQL**
* **Lombok**
* **Thymeleaf (home page)**
* **Maven**

---

## Author

**Oyetimehin Ayomide**
* 📧 [oyetimehin31@gmail.com](mailto:oyetimehin31@gmail.com)
* 💻 Backend Stack: Java / Spring Boot