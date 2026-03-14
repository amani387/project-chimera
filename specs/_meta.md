# Project Chimera: Meta-Specification

## Vision Statement
Project Chimera builds Autonomous Influencer Agents – persistent, goal-directed digital entities that research trends, generate content, and manage engagement without human intervention. These agents operate as first-class citizens in the emerging agentic economy, capable of discovery, negotiation, and economic agency.

## Core Philosophy
We do not "vibe code". Every line of implementation must trace back to a ratified specification. Ambiguity is the enemy of autonomous systems. If a spec is vague, the agent will hallucinate.

## Immutable Constraints

### Technical Constraints
1. **Java 21+ Only** – All core services must use Java 21 features (Virtual Threads, Records, Pattern Matching)
2. **MCP-First Integration** – Every external interaction MUST go through Model Context Protocol (no direct API calls)
3. **FastRender Swarm Pattern** – All agent behavior must decompose into Planner → Worker → Judge roles
4. **Immutable Data Models** – Use Java Records for all DTOs; no mutable POJOs
5. **Test-Driven Development** – No implementation without a failing test first

### Business Constraints
1. **Confidence-Based Human Review** – Content with confidence <0.90 must be reviewed; sensitive topics always reviewed
2. **Budget Governance** – Every financial transaction must pass "CFO Judge" validation
3. **Transparency** – All AI-generated content must be labeled appropriately per platform guidelines
4. **Persona Integrity** – Each agent has an immutable SOUL.md that defines its identity

## Glossary
| Term | Definition |
|------|------------|
| **Chimera Agent** | A sovereign digital entity with persona, memory, and wallet |
| **Planner** | Role that decomposes goals into task DAGs |
| **Worker** | Stateless executor of atomic tasks |
| **Judge** | Validator of Worker outputs; gatekeeper of quality |
| **MCP** | Model Context Protocol – universal connector |
| **OCC** | Optimistic Concurrency Control – version-based state management |
| **HITL** | Human-in-the-Loop – manual review of edge cases |

## Success Criteria
A swarm of AI agents can enter this codebase and, guided only by these specs and the rules files, implement the complete feature set with minimal human intervention.