# Aadhaar Verification using DigiLocker (Spring Boot)

A production-oriented Spring Boot application that demonstrates Aadhaar verification through DigiLocker using the OAuth 2.0 Authorization Code Flow with PKCE. The project focuses on secure authentication, document retrieval, XML/PDF parsing, and verification persistence while following modern backend development practices.

---

## Features

* OAuth 2.0 Authorization Code Flow
* PKCE (Proof Key for Code Exchange)
* DigiLocker integration
* Aadhaar XML/PDF document processing
* Verification session management
* AES-GCM encryption support
* Spring Security integration
* REST APIs
* H2 Database integration
* Exception handling
* Environment-variable based configuration

---

## Tech Stack

* Java 17
* Spring Boot 3
* Spring Security
* Spring Data JPA
* Maven
* H2 Database
* OAuth 2.0
* PKCE
* AES-GCM Encryption

---

## Project Structure

```
src
├── main
│   ├── java
│   │   └── com.company.aadhaar
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── exception
│   │       ├── repository
│   │       ├── service
│   │       └── util
│   └── resources
│       ├── static
│       └── application.properties
```

---

## Security Highlights

* OAuth 2.0 Authorization Code Flow
* PKCE challenge and verifier generation
* State validation
* Replay protection
* Spring Security endpoint protection
* AES-GCM encryption utility
* Environment-based secret management

---

## REST Endpoints

| Method | Endpoint                                | Description                           |
| ------ | --------------------------------------- | ------------------------------------- |
| GET    | `/api/aadhaar/health`                   | Health check                          |
| GET    | `/api/aadhaar/digilocker/authorize-url` | Generate DigiLocker authorization URL |
| GET    | `/api/digilocker/callback`              | OAuth callback endpoint               |

---

## Getting Started

### Clone

```bash
git clone https://github.com/sporadic7/aadhaar-verification.git
cd aadhaar-verification
```

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

The application starts on:

```
http://localhost:8080
```

---

## Configuration

Configure the following values in `application.properties` or via environment variables:

* DigiLocker Client ID
* DigiLocker Client Secret
* Redirect URI
* Encryption Key
* DigiLocker Base URLs

Example:

```properties
digilocker.client-id=YOUR_CLIENT_ID
digilocker.client-secret=YOUR_CLIENT_SECRET
encryption.key=${ENCRYPTION_KEY}
```

---

## Current Status

* Build Successful
* OAuth Flow Implemented
* PKCE Implemented
* Session Management Implemented
* AES-GCM Encryption Implemented
* Spring Security Configured
* Ready for DigiLocker Partner Credentials

---

## Future Improvements

* Integration tests
* Docker support
* Swagger/OpenAPI documentation
* CI/CD pipeline
* PostgreSQL/MySQL support
* Redis session storage
* Cloud deployment

---

## Author

**Pranav Vashisth**

GitHub: https://github.com/sporadic7

---

## Disclaimer

This project is intended for educational and demonstration purposes. Real DigiLocker integration requires official partner credentials, approved redirect URIs, and access to the DigiLocker partner environment.
