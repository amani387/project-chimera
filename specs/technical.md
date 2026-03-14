# Technical Specifications: API Contracts & Data Models

## 1. Data Models (Java Records)

### 1.1 Agent Persona (from SOUL.md)

```java
public record AgentPersona(
    String id,
    String name,
    List<String> voiceTraits,
    List<String> directives,
    String backstory,
    Map<String, Object> metadata
) {}
```

### 1.2 Task Definition (Planner → Worker)

```json
{
  "task_id": "uuid-v4",
  "task_type": "generate_content | reply_comment | execute_transaction | analyze_trends",
  "priority": "high | medium | low",
  "context": {
    "goal_description": "string",
    "persona_constraints": ["string"],
    "required_resources": ["mcp://resource/path"],
    "reference_materials": ["url1", "url2"]
  },
  "budget_allocation": {
    "max_cost_usdc": 0.0,
    "currency": "USDC"
  },
  "assigned_worker_id": "string (optional)",
  "created_at": "ISO-8601 timestamp",
  "status": "pending | in_progress | review | complete | failed"
}
```

### 1.3 Worker Result (Worker → Judge)

```json
{
  "task_id": "uuid-v4",
  "worker_id": "string",
  "output": {
    "content_type": "text | image | video | transaction",
    "content": "string or url",
    "artifacts": ["url1", "url2"]
  },
  "confidence_score": 0.95,
  "reasoning_trace": "Step-by-step explanation of generation process",
  "tool_calls": [
    {
      "tool": "mcp-server-name.toolName",
      "input": {},
      "output": {},
      "cost": 0.001
    }
  ],
  "execution_time_ms": 1234,
  "metadata": {}
}
```

### 1.4 Judge Verdict

```json
{
  "task_id": "uuid-v4",
  "judge_id": "string",
  "verdict": "approve | reject | escalate",
  "confidence_score": 0.92,
  "reasoning": "String explanation",
  "feedback_for_planner": "If rejected, guidance for retry",
  "state_version_checked": "string (hash or timestamp)"
}
```

### 1.5 Memory Entry

```java
public record MemoryEntry(
    String id,
    String agentId,
    String type, // "episodic" | "semantic" | "procedural"
    String content,
    List<Float> embedding,
    Map<String, Object> metadata,
    Instant timestamp,
    int accessCount,
    Instant lastAccessed
) {}
```

## 2. API Contracts

### 2.1 Orchestrator API (Internal)

#### GET /api/v1/agents

**Response:**

```json
{
  "agents": [
    {
      "id": "agent-001",
      "name": "FashionistaAI",
      "status": "active | idle | paused | error",
      "wallet_balance_usdc": 1250.5,
      "current_task": "optional task_id",
      "queue_depth": 3
    }
  ],
  "total_count": 42
}
```

#### POST /api/v1/agents

**Request:**

```json
{
  "persona_path": "agents/fashionista/SOUL.md",
  "initial_budget_usdc": 1000.0,
  "campaign_goals": ["Promote summer collection", "Engage with Gen-Z audience"]
}
```

#### POST /api/v1/tasks/{task_id}/review (HITL endpoint)

**Request:**

```json
{
  "reviewer_id": "human-123",
  "decision": "approve | reject | edit",
  "edited_content": "optional if edit",
  "feedback": "optional"
}
```

### 2.2 MCP Tool Definitions

#### Tool: generate_image

```json
{
  "name": "generate_image",
  "description": "Generate image from prompt with character consistency",
  "inputSchema": {
    "type": "object",
    "properties": {
      "prompt": { "type": "string" },
      "character_reference_id": { "type": "string" },
      "style": {
        "type": "string",
        "enum": ["realistic", "anime", "cinematic"]
      },
      "negative_prompt": { "type": "string" },
      "aspect_ratio": { "type": "string", "default": "16:9" }
    },
    "required": ["prompt", "character_reference_id"]
  }
}
```

#### Tool: post_to_social

```json
{
  "name": "post_to_social",
  "description": "Post content to connected social platforms",
  "inputSchema": {
    "type": "object",
    "properties": {
      "platform": {
        "type": "string",
        "enum": ["twitter", "instagram", "threads", "tiktok"]
      },
      "content": { "type": "string" },
      "media_urls": { "type": "array", "items": { "type": "string" } },
      "disclosure_level": {
        "type": "string",
        "enum": ["automated", "assisted", "none"]
      },
      "scheduled_time": { "type": "string", "format": "date-time" }
    },
    "required": ["platform", "content"]
  }
}
```

#### Tool: transfer_payment

```json
{
  "name": "transfer_payment",
  "description": "Send USDC/ETH to another wallet",
  "inputSchema": {
    "type": "object",
    "properties": {
      "to_address": { "type": "string", "pattern": "^0x[a-fA-F0-9]{40}$" },
      "amount": { "type": "number", "minimum": 0.000001 },
      "currency": { "type": "string", "enum": ["USDC", "ETH"] },
      "memo": { "type": "string" }
    },
    "required": ["to_address", "amount", "currency"]
  }
}
```

## 3. Database Schema

### 3.1 PostgreSQL (Relational Data)

```sql
-- Agents table
CREATE TABLE agents (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    persona_hash VARCHAR(64) NOT NULL,
    wallet_address VARCHAR(42),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    metadata JSONB
);

-- Campaigns
CREATE TABLE campaigns (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) REFERENCES agents(id),
    name VARCHAR(200) NOT NULL,
    goals JSONB NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    total_budget_usdc DECIMAL(20,6),
    spent_usdc DECIMAL(20,6) DEFAULT 0,
    status VARCHAR(20) NOT NULL
);

-- Content published
CREATE TABLE published_content (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) REFERENCES agents(id),
    platform VARCHAR(20) NOT NULL,
    external_id VARCHAR(100),
    content_type VARCHAR(20) NOT NULL,
    content_url TEXT,
    metadata JSONB,
    published_at TIMESTAMP NOT NULL,
    engagement_metrics JSONB
);

-- Tasks (for audit)
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(36) REFERENCES agents(id),
    task_type VARCHAR(50) NOT NULL,
    input JSONB,
    output JSONB,
    confidence_score DECIMAL(3,2),
    verdict VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    execution_time_ms INTEGER
);
```

### 3.2 Redis Schema (Ephemeral)

```text
# Task Queue
LPUSH task:queue:{agent_id} {task_json}

# Review Queue (for HITL)
LPUSH review:queue {task_result_json}

# Daily Spend Tracking
INCRBY daily:spend:{agent_id}:{YYYY-MM-DD} {amount_usdc_cents}

# Rate Limiting
INCR rate:limit:{platform}:{agent_id}:{minute}

# Short-term Memory (Episodic)
LPUSH memory:episodic:{agent_id}:{session_id} {memory_json}
EXPIRE memory:episodic:{agent_id}:{session_id} 3600  # 1 hour
```

### 3.3 Weaviate Schema (Vector DB)

```python
# Agent Memory Class
class_obj = {
    "class": "AgentMemory",
    "vectorizer": "none",  # We'll provide our own vectors
    "properties": [
        {"name": "agent_id", "dataType": ["string"]},
        {"name": "memory_type", "dataType": ["string"]},  # episodic, semantic
        {"name": "content", "dataType": ["text"]},
        {"name": "timestamp", "dataType": ["date"]},
        {"name": "metadata", "dataType": ["object"]},
        {"name": "access_count", "dataType": ["int"]}
    ]
}

# Trend Data Class
trend_class = {
    "class": "Trend",
    "properties": [
        {"name": "topic", "dataType": ["string"]},
        {"name": "source", "dataType": ["string"]},
        {"name": "volume", "dataType": ["int"]},
        {"name": "detected_at", "dataType": ["date"]},
        {"name": "related_articles", "dataType": ["text[]"]}
    ]
}
```

## 4. MCP Server Requirements

### Required MCP Servers

| Server              | Purpose                  | Source             |
| ------------------- | ------------------------ | ------------------ |
| mcp-server-twitter  | Social media interaction | Official or custom |
| mcp-server-weaviate | Vector memory storage    | Custom             |
| mcp-server-coinbase | AgentKit integration     | Coinbase           |
| mcp-server-redis    | Queue and cache          | Custom             |
| mcp-server-ideogram | Image generation         | Custom wrapper     |
| mcp-server-news     | RSS / News aggregation   | Custom             |

```java
public interface McpServer {
    String getName();
    List<McpTool> getTools();
    List<McpResource> getResources();
    List<McpPrompt> getPrompts();

    CompletableFuture<McpResult> callTool(String toolName, Map<String, Object> arguments);
    CompletableFuture<McpResourceResult> readResource(String resourceUri);
}

public record McpTool(
    String name,
    String description,
    JsonSchema inputSchema
) {}

public record McpResource(
    String uri,
    String name,
    String mimeType,
    String description
) {}
```

## 5. Concurrency Model

### Virtual Thread Configuration

```java
@Configuration
public class ConcurrencyConfig {

    @Bean
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public ExecutorService workerPool() {
        // For thousands of parallel Workers
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### Optimistic Concurrency Control (OCC)

```java
public record VersionedState<T>(
    T state,
    long version,
    Instant lastUpdated
) {
    public boolean canCommit(long expectedVersion) {
        return this.version == expectedVersion;
    }

    public VersionedState<T> withUpdatedState(T newState) {
        return new VersionedState<>(newState, this.version + 1, Instant.now());
    }
}
```

## 6. Configuration Properties

````yaml
# application.yml
chimera:
  agent:
    default-budget-usdc: 1000
    max-concurrent-tasks: 100
    confidence-thresholds:
      auto-approve: 0.90
      async-review: 0.70
      reject: 0.70
  memory:
    short-term-hours: 1
    semantic-top-k: 5
    weaviate-host: localhost:8080
  mcp:
    servers:
      - name: twitter
        command: "npx"
        args: ["-y", "@modelcontextprotocol/server-twitter"]
      - name: weaviate
        command: "java"
        args: ["-jar", "mcp-server-weaviate.jar"]
  blockchain:
    network: "base-sepolia"  # testnet for development
    max-daily-spend-usdc: 50
    ```
## 7. Error Handling
### Domain Exceptions
``` java
public sealed class ChimeraException extends RuntimeException {
    public record BudgetExceededException(String agentId, BigDecimal requested, BigDecimal available)
        extends ChimeraException {}

    public record ConfidenceTooLowException(String taskId, double confidence, double threshold)
        extends ChimeraException {}

    public record PersonaViolationException(String agentId, String violatedDirective)
        extends ChimeraException {}

    public record McpToolException(String toolName, String errorCode, String message)
        extends ChimeraException {}
}
````
