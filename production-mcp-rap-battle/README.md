# Production Ready MCP Rap Battle Agent

A fully production-ready, horizontally scalable platform where authenticated human users compete against an authoritative AI Rap Battle Agent via Next.js Web UI or through an authenticated Model Context Protocol (MCP) server.

## Architecture Highlights
- **Backend**: Node.js, Fastify, TypeScript, Zod, Drizzle ORM / PostgreSQL, Redis, WebSockets.
- **Frontend**: Next.js 14 (App Router), React, Tailwind CSS, TanStack Query, WebSockets.
- **MCP Server**: Compliant Model Context Protocol Server with OAuth 2.0 PKCE, scoped tokens, 13 battle tools, and read-only resources (`battle://`).
- **Scoring Subsystem**: 13 Canonical categories with exact weights totaling 100%, 1.5x final round multiplier, 2-decimal deterministic comparisons, and tiebreakers.
- **Battle Modes**: Classic, Freestyle, Themed (mandatory theme), Constraint (mandatory constraints), and Championship.

## Quick Start (Docker Compose)
```bash
cp .env.example .env
docker compose up -d --build
```
- Web Application: http://localhost:3000
- Fastify Backend API: http://localhost:4000
- MCP Server Endpoint: http://localhost:4000/mcp

## Running Tests
```bash
npm install
npm test
```
See `docs/` for complete architectural, scoring, MCP specification, and deployment documentation.
