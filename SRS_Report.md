# Software Requirements Specification (SRS)
## Multi-Level Secure File Storage System with Encrypted Vault and Segregated Databases

---

| Field         | Details                            |
|---------------|------------------------------------|
| **Project**   | SecureVault – Dual Authentication File Storage System |
| **Author**    | Jishan                             |
| **Version**   | 1.0                                |
| **Date**      | March 12, 2026                     |
| **Status**    | Development                        |

---

## 1. Introduction

### 1.1 Purpose
This SRS document describes the functional and non-functional requirements of **SecureVault**, a multi-level secure file storage system built with Spring Boot. The system is designed to demonstrate real-world cybersecurity concepts including dual-level authentication, AES-256 file encryption, JWT-based session management, two-factor authentication (2FA), and segregated database storage.

### 1.2 Scope
SecureVault allows authenticated users to upload, store, and retrieve files across two security levels. Files stored under Level 1 (normal access) are saved in plaintext, while files stored under Level 2 (secure access) are encrypted using AES-256 before being persisted to disk. The system enforces brute-force protection, OTP-based two-factor authentication, and full security audit logging.

### 1.3 Definitions
| Term         | Meaning                                                         |
|--------------|-----------------------------------------------------------------|
| Level 1      | Standard access using `passwordLevel1`; reads/writes normal files |
| Level 2      | Elevated access using `passwordLevel2`; reads/writes encrypted files |
| JWT          | JSON Web Token — stateless session token issued after login     |
| OTP          | One-Time Password sent via email for 2FA verification          |
| AES-256      | Advanced Encryption Standard with 256-bit key                  |
| BCrypt       | Password hashing algorithm used to store credentials securely  |

### 1.4 Technology Stack
| Layer          | Technology                        |
|----------------|-----------------------------------|
| Backend        | Java 17, Spring Boot 3.x          |
| Security       | Spring Security, JWT (JJWT)       |
| Database       | MySQL (`securevault_db`)          |
| Encryption     | AES-256 (Java Cipher API)         |
| Password Hash  | BCrypt                            |
| 2FA / Email    | JavaMail (Gmail SMTP)             |
| Frontend       | HTML, CSS, JavaScript (static)    |

---

## 2. Overall Description

### 2.1 System Overview
```
User (Browser)
     │
     ▼
┌─────────────────────────────────────────────┐
│              Spring Boot Backend             │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │AuthCtrl  │  │FileCtrl  │  │JwtFilter  │  │
│  └────┬─────┘  └────┬─────┘  └─────┬─────┘  │
│       │             │              │         │
│  ┌────▼─────┐  ┌────▼─────┐  ┌────▼──────┐  │
│  │AuthSvc   │  │FileSvc   │  │JwtUtil    │  │
│  └────┬─────┘  └────┬─────┘  └───────────┘  │
│       │             │                        │
│  ┌────▼─────┐  ┌────▼──────────────────────┐ │
│  │ MySQL DB │  │  /storage/normal/          │ │
│  │ users    │  │  /storage/secure/ (AES-256)│ │
│  │ files    │  └───────────────────────────┘ │
│  └──────────┘                                │
└─────────────────────────────────────────────┘
```

### 2.2 User Roles
| Role         | Description                                             |
|--------------|---------------------------------------------------------|
| Guest        | Can register a new account                             |
| Level 1 User | Logged in with Password 1 — accesses normal file vault |
| Level 2 User | Logged in with Password 2 — accesses encrypted vault   |

### 2.3 Assumptions & Constraints
- MySQL must be running locally on port `3306`.
- Gmail SMTP credentials must be configured for 2FA OTP delivery.
- File upload size is limited to **50 MB** per file.
- JWT tokens expire after **1 hour**.
- OTP is valid for **5 minutes**.

---

## 3. Functional Requirements

### FR-01 — User Registration
- User provides `username`, `passwordLevel1`, `passwordLevel2`, `email`, `securityQuestion`, and `securityAnswer`.
- Both passwords are hashed with **BCrypt** before storage.
- Security answer is also BCrypt-hashed.

### FR-02 — Dual-Level Authentication
- **Level 1 Login**: validates `passwordLevel1` → issues JWT with `accessLevel = LEVEL1`.
- **Level 2 Login**: validates `passwordLevel2` → issues JWT with `accessLevel = LEVEL2`.
- If 2FA is enabled, an OTP is emailed; user must submit OTP to receive the final JWT.

### FR-03 — Two-Factor Authentication (2FA)
- User can enable/disable 2FA from the dashboard.
- On login, if 2FA is enabled, a 6-digit OTP is sent to the registered email.
- OTP expires after 5 minutes. Failed OTP resets the flow.

### FR-04 — Brute Force Protection
- After **5 consecutive failed login attempts**, the account is **locked**.
- Locked accounts cannot log in until unlocked via the password recovery flow.

### FR-05 — Password Recovery
- User answers their registered security question.
- If correct, they may set a new `passwordLevel1` and `passwordLevel2`.

### FR-06 — File Upload
- Authenticated users upload files via the dashboard.
- **Level 1**: file is stored as-is under `/storage/normal/`.
- **Level 2**: file is **AES-256 encrypted** before being saved under `/storage/secure/`.
- File metadata (name, owner, path, access level, upload time) is stored in MySQL.

### FR-07 — File Download
- Users can list and download their own files.
- **Level 2** files are decrypted on-the-fly before being returned to the browser.
- Users cannot access files belonging to other users.

### FR-08 — File Delete
- Users can delete their own files.
- Deletes both the physical file from disk and the metadata from the database.

### FR-09 — JWT-Based Session Management
- Every protected API endpoint requires a valid `Authorization: Bearer <token>` header.
- `JwtFilter` intercepts every request, validates the token, and injects the security context.

---

## 4. Non-Functional Requirements

| ID     | Category       | Requirement                                                         |
|--------|----------------|---------------------------------------------------------------------|
| NFR-01 | Security       | All passwords stored as BCrypt hashes; never stored in plaintext    |
| NFR-02 | Security       | Sensitive files encrypted with AES-256 at rest                      |
| NFR-03 | Security       | JWT signed with HMAC-SHA256; expiry enforced server-side            |
| NFR-04 | Availability   | Application runs on `localhost:8080`; accessible via any browser    |
| NFR-05 | Performance    | File upload/download under 3 seconds for files up to 10 MB         |
| NFR-06 | Scalability    | MySQL schema auto-updated via Hibernate DDL                         |
| NFR-07 | Usability      | Single-page HTML/CSS/JS frontend; no framework dependency           |
| NFR-08 | Auditability   | Every login, upload, download, and failure logged in application log |

---

## 5. System Components

| Component               | Class / File                   | Responsibility                              |
|-------------------------|--------------------------------|---------------------------------------------|
| Entry Point             | `SecurevaultApplication.java`  | Boots Spring context                        |
| Auth Controller         | `AuthController.java`          | Register, login, OTP, recovery endpoints    |
| File Controller         | `FileController.java`          | Upload, download, list, delete endpoints    |
| Auth Service            | `AuthService.java`             | Business logic for auth & account management|
| File Service            | `FileService.java`             | File I/O, storage path routing              |
| Encryption Service      | `EncryptionService.java`       | AES-256 encrypt / decrypt                   |
| Two-Factor Service      | `TwoFactorService.java`        | OTP generation & email dispatch             |
| JWT Utility             | `JwtUtil.java`                 | Token generation, validation, claim parsing |
| JWT Filter              | `JwtFilter.java`               | Intercepts HTTP requests, validates tokens  |
| Security Config         | `SecurityConfig.java`          | Spring Security rules, CORS, public routes  |
| DataSource Config       | `DataSourceConfig.java`        | MySQL datasource configuration              |
| User Entity             | `User.java`                    | DB model: credentials, 2FA, lock status     |
| FileMetadata Entity     | `FileMetadata.java`            | DB model: file name, owner, level, path     |

---

## 6. Use Case Summary

```
┌─────────────────────────────────────────────────────┐
│                      SecureVault                     │
│                                                     │
│  [User]─────► Register                              │
│  [User]─────► Login (Level 1 / Level 2)             │
│       └──────► [If 2FA] Submit OTP                  │
│  [Level1 User]► Upload / Download / Delete Normal Files │
│  [Level2 User]► Upload / Download / Delete Encrypted Files │
│  [User]─────► Enable / Disable 2FA                  │
│  [User]─────► Recover Password via Security Question │
└─────────────────────────────────────────────────────┘
```

---

## 7. Data Flow — Secure File Upload (Level 2)

```
Browser ──[POST /api/files/upload + JWT(LEVEL2)]──► FileController
                                                         │
                                                    FileService
                                                         │
                                               EncryptionService (AES-256)
                                                         │
                                             /storage/secure/<timestamp>_<filename>
                                                         │
                                               FileMetadataRepository
                                                         │
                                                  MySQL → file_metadata table
```

---

*End of SRS Document — SecureVault v1.0*

