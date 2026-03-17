# Project Chimera – Autonomous Influencer Network

---

## 🚀 Overview

Project Chimera is a production-ready platform for creating and managing autonomous AI influencers. Each agent (a *Chimera*) can:

* Research trends
* Generate multimodal content
* Engage with audiences
* Transact autonomously using a built-in crypto wallet

The system is built on a **spec-driven, test-first foundation** and designed to scale to thousands of concurrent agents using:

* Java 21 virtual threads
* FastRender swarm pattern *(Planner → Worker → Judge)*

---

## ✨ Key Features

### 🧠 Autonomous Agent Swarm

Hierarchical **Planner / Worker / Judge** architecture for reliable, scalable execution.

### 📜 Spec-Driven Development

All functionality is defined in the `specs/` folder:

* `_meta.md`
* `functional.md`
* `technical.md`
* `openclaw_integration.md`

### 🔌 Model Context Protocol (MCP)

All integrations (Twitter, Weaviate, Coinbase, news) go through **MCP servers** — no hard-coded APIs.

### 🧬 Persistent Memory

* Uses **Weaviate vector DB**
* Supports both **mock** and **real** instances

### 🎭 Persona System

Each agent has a unique personality defined in a `SOUL.md` file:

* YAML frontmatter
* Backstory
* Context assembled dynamically

### 👨‍⚖️ Human-in-the-Loop

Confidence-based routing:

* **0.7–0.9 → human review dashboard**

### 💰 Agentic Commerce *(Planned)*

Non-custodial wallets via Coinbase AgentKit.

### 🧪 Test-Driven Development

* JUnit 5
* CI pipeline runs tests automatically

---

## 🏗️ Architecture Overview

Core loop (FastRender swarm pattern):

```
Campaign Goal
   ↓
Planner
   ↓
Task Queue (Redis)
   ↓
Worker
   ↓
Review Queue (Redis)
   ↓
Judge
   ↓
Approved / Rejected / Escalated
```

### Components

**Planner**

* Reads high-level goals
* Builds a DAG of tasks
* Pushes to Redis

**Worker**

* Stateless
* Runs on virtual threads
* Executes tasks via MCP tools

**Judge**

* Validates outputs
* Decides:

  * Approve
  * Reject
  * Escalate

### Integrations (via MCP)

* Weaviate (current)
* Twitter *(future)*
* Coinbase *(future)*
* News APIs *(future)*

---

## 🧰 Tech Stack

| Area        | Technologies                                                      |
| ----------- | ----------------------------------------------------------------- |
| Backend     | Java 21, Spring Boot, Virtual Threads, Spring Data JPA, Hibernate |
| Messaging   | Redis (Lettuce), Spring Data Redis                                |
| Database    | PostgreSQL, Weaviate, Redis                                       |
| MCP Clients | MCP Java SDK, custom HTTP clients                                 |
| AI/LLM      | OpenAI / Gemini / Ollama *(planned)*                              |
| Frontend    | React, Next.js, Tailwind CSS *(future)*                           |
| DevOps      | Docker, Docker Compose, GitHub Actions, Testcontainers            |
| Testing     | JUnit 5, Mockito, Testcontainers, RestAssured *(planned)*         |

---

## 📁 Project Structure

```
chimera/
├── .github/
│   └── workflows/
├── specs/
│   ├── _meta.md
│   ├── functional.md
│   ├── technical.md
│   └── openclaw_integration.md
├── src/
│   ├── main/java/com/chimera/
│   │   ├── planner/
│   │   ├── worker/
│   │   ├── judge/
│   │   ├── mcp/
│   │   ├── model/
│   │   ├── service/
│   │   └── config/
│   └── resources/
│       ├── agents/
│       ├── application.yml
│       └── goals.json
├── test/
├── scripts/
├── docker-compose.weaviate.yml
├── Dockerfile
├── Makefile
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### ✅ Prerequisites

* Java 21+
* Maven (or `mvnw`)
* Docker
* Redis

---

### ⚡ Quick Start

#### 1. Clone repo

```bash
git clone https://github.com/amani387/project-chimera.git
cd project-chimera
```

#### 2. Build

```bash
./mvnw clean install -DskipTests
```

#### 3. Run Redis

```bash
docker run -d -p 6379:6379 redis:7-alpine
```

#### 4. Run Weaviate (optional)

```bash
docker-compose -f docker-compose.weaviate.yml up -d
```

#### 5. Run tests

```bash
./mvnw test
```

#### 6. Start app

```bash
./mvnw spring-boot:run
```

Visit:

```
http://localhost:8080/api/health
```

---

## 🧪 Testing

### Strategy

* **Unit tests** → fast, isolated
* **Integration tests** → Testcontainers
* **Conditional tests** → require Docker

### Commands

Run all:

```bash
./mvnw test
```

Run specific:

```bash
./mvnw test -Dtest=WeaviateRealIntegrationTest
```

---

## ⚙️ Configuration

`application.yml`

```yaml
chimera:
  redis:
    host: localhost
    port: 6379

  weaviate:
    url: http://localhost:8080
    class-name: Memory
    mock: true
    connect-timeout: 5000
    read-timeout: 10000

  agents:
    directory: agents/

  planner:
    context-memory-limit: 5
```

### Switching Weaviate mode

* `mock: true` → in-memory
* `mock: false` → real instance required

---

## 📜 Persona Files (SOUL.md)

Example:

```yaml
id: fashion-001
name: FashionistaAI

voice_traits:
  - witty
  - trendy
  - gen-z

directives:
  - never promote fast fashion
  - use inclusive language

metadata:
  style: streetwear
  favorite_brands: [Patagonia, Veja]
```

```
I'm a virtual fashion influencer who loves sustainable style and streetwear...
```

---

## 🔮 Roadmap

| Phase | Feature              | Status     |
| ----- | -------------------- | ---------- |
| 1     | Core swarm           | ✅ Complete |
| 2.1   | Weaviate (mock)      | ✅ Complete |
| 2.2   | Real Weaviate client | ✅ Complete |
| 3     | Persona system       | ✅ Complete |
| 4     | LLM integration      | 🔜 Next    |
| 5     | Agentic commerce     | ⏳ Planned  |
| 6     | HITL dashboard       | ⏳ Planned  |
| 7     | Production hardening | ⏳ Planned  |

---

See:

```
.github/copilot-instructions.md
```

---


