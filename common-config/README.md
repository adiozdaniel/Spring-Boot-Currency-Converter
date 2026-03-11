# 🏗️ Common Config & Contracts

Centralized repository for gRPC contracts and shared configurations for the Currency Converter Ecosystem.

## 📋 Overview

This module serves as the "Source of Truth" for all inter-service communication. It contains the Protocol Buffer (`.proto`) definitions and generates the Java stubs used by both gRPC servers and clients.

## 📁 Structure

```text
common-config/
├── src/main/proto/       # Protobuf definitions
│   ├── auth.proto        # Authentication Service contract
│   ├── main.proto        # Conversion Service contract
│   └── rate.proto        # Rate Service contract
└── pom.xml               # Code generation logic
```

## 📡 Contracts

### `auth.proto`

Defines the `AuthService` used for identity verification and token validation.

### `main.proto`

Defines the `ConversionService` for orchestration and history management.

### `rate.proto`

Defines the `RateService` for exchange rate lookups.

## ⚙️ Code Generation

The Java source code is automatically generated during the Maven `compile` phase using the `protobuf-maven-plugin`.

**Generated Package:** `com.currencyconverter.common.grpc`

### Build Command

```bash
mvn clean compile -pl common-config -am
```

## 🛠️ Tech Stack

- **Protocol Buffers**: 4.29.3
- **gRPC**: 1.70.0
- **Java**: 21
