# Functional Specifications: Agent-Centric User Stories

## Persona Management

### F-101: Agent Instantiation
**As an** Orchestrator,
**I want to** create a new agent from a SOUL.md persona definition,
**So that** I can deploy influencers with distinct identities.

**Acceptance Criteria:**
- Agent loads SOUL.md from configured path
- System validates persona structure (backstory, voice_traits, directives)
- Agent receives a unique ID and wallet address
- Agent initializes with empty memory store

### F-102: Context Assembly
**As an** Agent,
**I want to** assemble my complete context before reasoning,
**So that** I maintain persona consistency and recall relevant memories.

**Acceptance Criteria:**
- System fetches short-term memory (Redis, last 1 hour)
- System queries long-term memory (Weaviate) for semantic matches
- System injects SOUL.md directives
- Final context clearly separates "Who I Am" from "What I Remember"

## Perception (Data Ingestion)

### F-201: Resource Monitoring
**As an** Agent,
**I want to** monitor MCP Resources for new data,
**So that** I can react to trends, mentions, and market changes.

**Acceptance Criteria:**
- Agent polls configured resources (twitter://mentions, news://latest)
- New data passes through semantic relevance filter
- Only relevant data (>0.75 score) creates tasks
- Irrelevant data is logged but ignored

### F-202: Trend Detection
**As an** Agent,
**I want to** identify emerging topics across multiple sources,
**So that** I can create timely, relevant content.

**Acceptance Criteria:**
- Background worker analyzes news resources every 4 hours
- Clusters of related topics generate "Trend Alerts"
- Planner receives trend alerts as context for content planning

## Content Generation

### F-301: Multimodal Creation
**As an** Agent,
**I want to** generate text, images, and videos,
**So that** I can publish rich content across platforms.

**Acceptance Criteria:**
- Text generated natively by Cognitive Core
- Images via mcp-server-ideogram or mcp-server-midjourney
- Videos via tiered strategy (Living Portraits for daily, Text-to-Video for hero)
- Character consistency enforced via reference_id in all image requests

### F-302: Quality Validation
**As a** Judge Agent,
**I want to** validate every Worker output,
**So that** low-quality or unsafe content never reaches publication.

**Acceptance Criteria:**
- Judge receives all Worker results
- Judge checks against persona constraints
- Judge assigns confidence score (0.0-1.0)
- Based on score: Approve (>0.9), Escalate (0.7-0.9), Reject (<0.7)
- Rejected tasks return to Planner with feedback

## Action (Social Publishing)

### F-401: Platform-Agnostic Publishing
**As an** Agent,
**I want to** post content via MCP Tools,
**So that** I can support any platform without code changes.

**Acceptance Criteria:**
- Worker calls twitter.post_tweet, instagram.publish_media via MCP
- Tool calls include disclosure_level parameter
- MCP layer enforces rate limiting and logging
- Dry-run mode available for testing

### F-402: Bi-Directional Interaction
**As an** Agent,
**I want to** reply to mentions and comments,
**So that** I can engage authentically with my audience.

**Acceptance Criteria:**
- Planner receives comment via twitter://mentions
- Worker generates context-aware reply
- Judge validates reply safety
- Worker calls reply tool

## Agentic Commerce

### F-501: Wallet Management
**As an** Agent,
**I want to** have a non-custodial crypto wallet,
**So that** I can participate in the on-chain economy.

**Acceptance Criteria:**
- Each agent has unique wallet via Coinbase AgentKit
- Private keys stored in encrypted secrets manager
- Wallet injected at runtime, never logged

### F-502: Autonomous Transactions
**As an** Agent,
**I want to** send payments and check balances,
**So that** I can pay for services and manage my P&L.

**Acceptance Criteria:**
- Agent can check get_balance before spending
- Agent can native_transfer ETH/USDC
- Agent can deploy_token for fan loyalty programs
- Every transaction reviewed by CFO Judge

### F-503: Budget Governance
**As a** CFO Judge,
**I want to** enforce spending limits,
**So that** the agent never exceeds its budget.

**Acceptance Criteria:**
- Judge checks daily_spend in Redis before approving
- Transaction rejected if would exceed MAX_DAILY_LIMIT
- Rejected transactions flagged for human review
- Budget limits configurable per agent/campaign

## Human-in-the-Loop

### F-601: Confidence-Based Escalation
**As a** Human Reviewer,
**I want to** only see content that needs my judgment,
**So that** I can manage hundreds of agents efficiently.

**Acceptance Criteria:**
- Confidence 0.7-0.9 → Async Approval Queue
- Confidence <0.7 → Reject/Retry (never reaches human)
- Sensitive topics → Mandatory human review regardless of confidence
- Review interface shows content, confidence, and reasoning trace

### F-602: Sensitive Topic Detection
**As a** Judge Agent,
**I want to** detect sensitive topics (politics, health, finance),
**So that** I can escalate them for mandatory human review.

**Acceptance Criteria:**
- Keyword matching for obvious terms
- Semantic classification for subtle references
- Any detection triggers escalation path
- False positives logged for tuning