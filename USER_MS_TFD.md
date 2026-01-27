# Technical Functional Design (TFD) - User Microservice

## 1. Introduction
The **User Service** is a core microservice in the E-commerce architecture responsible for managing user identities, profiles, and lifecycle events. It handles user registration, email verification, and profile management (CRUD), while integrating with other services via Kafka events.

## 2. Architecture & Technology Stack
*   **Language**: Java 17
*   **Framework**: Spring Boot 3.3.2
*   **Database**: MySQL 8.0 (Persistence)
*   **Messaging**: Apache Kafka (Event-Driven Architecture)
*   **Build Tool**: Maven
*   **Containerization**: Docker

## 3. Functional Requirements
1.  **User Registration**: Allow new users to sign up with profile details.
2.  **Email Verification**: Publish events to trigger email verification (via Notification Service).
3.  **Profile Management**: View, update, and delete user profiles.
4.  **Event Publishing**: Notify other services (e.g., Order, Payment) when user data changes.

## 4. Technical Design

### 4.1. Database Schema
**Table**: `users`

| Column Name       | Data Type    | Constraints | Description |
|-------------------|--------------|-------------|-------------|
| `id`              | BIGINT       | PK, AI      | Unique User ID |
| `username`        | VARCHAR(255) | Not Null    | User's login name |
| `email`           | VARCHAR(255) | Not Null    | User's email address |
| `password`        | VARCHAR(255) | Not Null    | BCrypt encrypted password |
| `first_name`      | VARCHAR(255) | -           | User's first name |
| `last_name`       | VARCHAR(255) | -           | User's last name |
| `phone_number`    | VARCHAR(255) | -           | Contact number |
| `address`         | VARCHAR(255) | -           | Shipping/Billing address |
| `is_verified`     | BOOLEAN      | Default 0   | Email verification status |
| `verification_token`| VARCHAR(255)| -           | Token for email verification |
| `created_at`      | BIGINT       | -           | Timestamp of creation |

### 4.2. API Specifications
Base URL: `/api/users`

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| `POST` | `/register` | Register a new user | `User` JSON | Success Message |
| `GET` | `/verify` | Verify email via token | `token` (param) | Success Message |
| `GET` | `/` | Get all users | - | List of `User` |
| `GET` | `/{id}` | Get user by ID | - | `User` JSON |
| `PUT` | `/{id}` | Update user profile | `User` JSON | Updated `User` |
| `DELETE`| `/{id}` | Delete user | - | Success Message |

### 4.3. Kafka Integration
The service acts as a **Producer** for the following topics:

1.  **Topic**: `emailVerificationTopic`
    *   **Trigger**: User Registration.
    *   **Payload Format**: `email,token,username`
    *   **Purpose**: Consumed by Notification Service to send verification emails.

2.  **Topic**: `user-events`
    *   **Trigger**: User Update, User Deletion.
    *   **Key**: `USER_UPDATED` or `USER_DELETED`
    *   **Payload**: User JSON (for updates) or User ID (for deletion).
    *   **Purpose**: Consumed by downstream services (Order, Cart) to maintain data consistency.

### 4.4. Security
*   **Authentication**: Current implementation allows public access to `/api/users/**` for development/testing ease.
*   **Password Storage**: Passwords are hashed using **BCryptPasswordEncoder** before storage.

## 5. Data Flow
1.  **Registration**:
    *   Client POSTs to `/register`.
    *   Service hashes password, generates verification token, saves to MySQL.
    *   Service publishes message to `emailVerificationTopic`.
2.  **Update**:
    *   Client PUTs to `/{id}`.
    *   Service updates MySQL record.
    *   Service publishes updated user data to `user-events`.
