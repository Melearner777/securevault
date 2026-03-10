# 🔐 SecureVault — Multi-Level Secure File Storage System

A **cybersecurity-focused** file storage web application built with **Spring Boot**, featuring dual-level authentication, AES file encryption, JWT-based sessions, 2FA via email OTP, and segregated databases for normal and sensitive files.

---

## 🧠 Concept

This project demonstrates **Security Level Segregation** — a concept used in banking, military, and classified storage systems.

| Level | Access | Storage |
|-------|--------|---------|
| Level 1 (Password 1) | Normal files | `normal` storage |
| Level 2 (Password 2) | Sensitive/encrypted files | `secure` storage |

Even if one credential is compromised, the other level remains protected.

---

## 🛡️ Security Features

- ✅ **Dual-level authentication** — two separate passwords grant different access levels
- ✅ **AES-256 file encryption** — Level 2 files are encrypted at rest
- ✅ **JWT authentication** — stateless, secure session management
- ✅ **BCrypt password hashing** — passwords never stored in plain text
- ✅ **Two-Factor Authentication (2FA)** — email OTP on login
- ✅ **Brute-force protection** — account lock after failed attempts
- ✅ **Segregated file storage** — `/storage/normal/` and `/storage/secure/`
- ✅ **Security logging** — all actions tracked

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3 |
| Security | Spring Security + JWT |
| Database | MySQL |
| Encryption | AES-256 |
| Password Hashing | BCrypt |
| Email / 2FA | JavaMail (Gmail SMTP) |
| Frontend | HTML, CSS, JavaScript |

---

## 📁 Project Structure

```
src/main/java/com/jishan/securevault/
├── config/          # Security & DataSource configuration
├── controller/      # REST API endpoints (Auth, File)
├── entity/          # JPA entities (User, FileMetadata)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, UserDetailsService
├── service/         # Business logic (Auth, File, Encryption, 2FA)
└── util/            # Utility classes
```

---

## ⚙️ Setup & Run

### Prerequisites
- Java 17+
- Maven
- MySQL running locally

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/securevault.git
   cd securevault
   ```

2. **Configure the application**
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   Then edit `application.properties` and fill in:
   - Your MySQL password
   - Your Gmail address and App Password (for 2FA OTP)

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Open in browser**
   ```
   http://localhost:8080
   ```

---

## 🔑 How It Works

1. **Register** with a username, email, password (Level 1), and optionally a second password (Level 2)
2. **Login with Password 1** → access your normal files
3. **Login with Password 2** → access your encrypted secure vault
4. **Enable 2FA** in the dashboard → OTP sent to your email on every login
5. **Upload files** → Level 2 files are automatically AES-encrypted before storage

---

## 📸 Screenshots

> Dashboard with file upload and 2FA management

*(Add screenshots here)*

---

## ⚠️ Disclaimer

This project is built for educational purposes to demonstrate cybersecurity concepts. Do not use in production without a thorough security audit.

---

## 👨‍💻 Author

**Jishan** — Java Developer & Cybersecurity Enthusiast

