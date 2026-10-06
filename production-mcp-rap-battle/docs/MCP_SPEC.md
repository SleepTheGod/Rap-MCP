# Model Context Protocol (MCP) Server Specification

## Authentication Flow (OAuth 2.0 + PKCE)
1. Remote client initiates authorization with `code_challenge` (S256).
2. Authenticated user reviews requested scopes (`battle.read`, `battle.submit`, etc.).
3. Server returns authorization code to client's registered redirect URI.
4. Client exchanges authorization code and `code_verifier` for audience-restricted access token.
5. All subsequent tool calls include `Authorization: Bearer <mcp_token>`.

## 13 Standard MCP Tools
1. `start_battle`: Initializes an authoritative battle state.
2. `configure_battle`: Sets format, round count, mode, difficulty, theme, and constraints.
3. `submit_verse`: Submits authenticated user verse for active turn.
4. `generate_ai_response`: Triggers AI generation for the AI turn.
5. `evaluate_verse`: Invokes independent judge scoring.
6. `get_battle_state`: Retrieves authoritative state machine position.
7. `get_current_round`: Retrieves round specifics.
8. `get_scores`: Retrieves category scores, round scores, and final weighted totals.
9. `get_statistics`: Retrieves cumulative battle stats (punchline avg, rhyme avg, etc.).
10. `end_battle`: Authoritatively terminates or concedes a battle.
11. `restart_battle`: Creates a clean rematch with identical settings.
12. `get_history`: Retrieves list of user's past battles.
13. `get_transcript`: Retrieves verbatim chronological transcript.

## MCP Resources
- `battle://state/{battleId}`
- `battle://transcript/{battleId}`
- `battle://scores/{battleId}`
- `battle://statistics/{battleId}`
