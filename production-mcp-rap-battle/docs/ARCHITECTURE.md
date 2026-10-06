# Architecture Documentation

## Logical Components
1. **Web Client (Next.js 14 App Router)**: Provides the arena UI, live timer, verse submission editor with line/char counting, real-time scoreboard, judge feedback breakdown, and battle history.
2. **API Layer (Fastify + TypeScript)**: Authoritative boundary. Handles authentication, battle transitions, verse acceptance, score queries, and audit logging.
3. **Authentication & Authorization**: Session-based auth with secure cookies for browser users, plus OAuth 2.0 PKCE authentication for remote MCP clients.
4. **Battle Engine**: Authoritative finite state machine ensuring monotonic progression across states (`Created -> Waiting -> UserTurn -> AiTurn -> Judging -> RoundComplete -> Completed`).
5. **AI Provider Abstraction**: Generates original opponent verses based on battle history, opponent lines, theme, constraints, and selected difficulty.
6. **Judging Subsystem**: Independent evaluator implementing the canonical 13 categories, 100-point total weight, and First Turn Rebuttal rule ("Response Quality").
7. **MCP Server**: Implements the Model Context Protocol specification with tool authorization, scoped permissions, and resource URIs (`battle://`).
8. **Persistence Layer (PostgreSQL + Drizzle/Prisma)**: Authoritative source of truth for all entities.
9. **Realtime Transport (WebSockets)**: Event-driven monotonic updates.
10. **Observability**: Structured Pino logging, OpenTelemetry tracing spans, metrics, and health/readiness endpoints.
