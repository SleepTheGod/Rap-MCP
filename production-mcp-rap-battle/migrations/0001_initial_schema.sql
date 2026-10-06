-- PostgreSQL Initial Schema for Production MCP Rap Battle Agent
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    session_version INT NOT NULL DEFAULT 1,
    authentication_method VARCHAR(50) NOT NULL DEFAULT 'Password'
);

CREATE TABLE battles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'Created',
    mode VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    current_round_number INT NOT NULL DEFAULT 1,
    current_turn VARCHAR(50) NOT NULL DEFAULT 'User',
    starting_side VARCHAR(50) NOT NULL DEFAULT 'User',
    configured_round_count INT NOT NULL DEFAULT 5,
    completed_round_count INT NOT NULL DEFAULT 0,
    current_round_id UUID,
    battle_version INT NOT NULL DEFAULT 1,
    turn_started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    turn_expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    failure_code VARCHAR(100),
    failure_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE battle_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE UNIQUE,
    round_count INT NOT NULL DEFAULT 5,
    verse_minimum_lines INT NOT NULL DEFAULT 1,
    verse_maximum_lines INT NOT NULL DEFAULT 16,
    verse_maximum_characters INT NOT NULL DEFAULT 16000,
    turn_timeout_seconds INT NOT NULL DEFAULT 90,
    mode VARCHAR(50) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    theme VARCHAR(100),
    constraints JSONB NOT NULL DEFAULT '[]'::jsonb,
    starting_side VARCHAR(50) NOT NULL DEFAULT 'User',
    final_round_weight NUMERIC(4,2) NOT NULL DEFAULT 1.50,
    enabled_scoring_categories JSONB NOT NULL,
    scoring_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    judge_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    ai_generation_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    battle_rules_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rounds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE,
    round_number INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    user_verse_id UUID,
    ai_verse_id UUID,
    user_score NUMERIC(5,2),
    ai_score NUMERIC(5,2),
    weighted_user_score NUMERIC(6,2),
    weighted_ai_score NUMERIC(6,2),
    round_weight NUMERIC(4,2) NOT NULL DEFAULT 1.00,
    winner VARCHAR(50),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    version INT NOT NULL DEFAULT 1,
    UNIQUE(battle_id, round_number)
);

CREATE TABLE turns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    sequence_number INT NOT NULL,
    competitor VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    submission_id UUID,
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE verses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    competitor VARCHAR(50) NOT NULL,
    sequence_number INT NOT NULL,
    content TEXT NOT NULL,
    line_count INT NOT NULL,
    character_count INT NOT NULL,
    submission_status VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    timed_out BOOLEAN NOT NULL DEFAULT FALSE,
    generation_provider VARCHAR(50),
    generation_model VARCHAR(100),
    generation_request_id VARCHAR(100),
    generation_latency_ms INT,
    content_policy_status VARCHAR(50) NOT NULL DEFAULT 'Passed',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE judgments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    verse_id UUID NOT NULL REFERENCES verses(id) ON DELETE CASCADE,
    competitor VARCHAR(50) NOT NULL,
    scoring_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    judge_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    normalized_score NUMERIC(5,2) NOT NULL,
    rationale TEXT NOT NULL,
    strengths JSONB NOT NULL DEFAULT '[]'::jsonb,
    weaknesses JSONB NOT NULL DEFAULT '[]'::jsonb,
    model_identifier VARCHAR(100) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE judgment_category_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    judgment_id UUID NOT NULL REFERENCES judgments(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    score INT NOT NULL,
    weight INT NOT NULL,
    weighted_contribution NUMERIC(5,2) NOT NULL,
    explanation TEXT NOT NULL
);

CREATE TABLE scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    competitor VARCHAR(50) NOT NULL,
    raw_verse_score NUMERIC(5,2) NOT NULL,
    round_weight NUMERIC(4,2) NOT NULL,
    weighted_round_score NUMERIC(6,2) NOT NULL,
    cumulative_weighted_score NUMERIC(7,2) NOT NULL,
    battle_score_at_calculation NUMERIC(5,2) NOT NULL,
    scoring_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE battle_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE,
    round_id UUID,
    turn_id UUID,
    actor_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sequence_number INT NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(100) NOT NULL,
    causation_id VARCHAR(100)
);

CREATE TABLE battle_statistics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    battle_id UUID NOT NULL REFERENCES battles(id) ON DELETE CASCADE UNIQUE,
    total_rounds INT NOT NULL,
    user_rounds_won INT NOT NULL DEFAULT 0,
    ai_rounds_won INT NOT NULL DEFAULT 0,
    drawn_rounds INT NOT NULL DEFAULT 0,
    user_average_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    ai_average_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    user_highest_round_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    ai_highest_round_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    user_punchline_avg NUMERIC(4,2) NOT NULL DEFAULT 0,
    ai_punchline_avg NUMERIC(4,2) NOT NULL DEFAULT 0,
    user_rhyme_avg NUMERIC(4,2) NOT NULL DEFAULT 0,
    ai_rhyme_avg NUMERIC(4,2) NOT NULL DEFAULT 0,
    user_rebuttal_avg NUMERIC(4,2) NOT NULL DEFAULT 0,
    ai_rebuttal_avg NUMERIC(4,2) NOT NULL DEFAULT 0,
    total_verses INT NOT NULL DEFAULT 0,
    timeout_count INT NOT NULL DEFAULT 0,
    battle_duration_seconds INT NOT NULL DEFAULT 0,
    final_user_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    final_ai_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE idempotency_records (
    key VARCHAR(255) PRIMARY KEY,
    principal_id VARCHAR(100) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    response_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    principal_id VARCHAR(100) NOT NULL,
    principal_type VARCHAR(50) NOT NULL,
    client_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(50),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE mcp_clients (
    client_id VARCHAR(100) PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    client_type VARCHAR(50) NOT NULL DEFAULT 'Public',
    redirect_uris JSONB NOT NULL DEFAULT '[]'::jsonb,
    allowed_scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE mcp_auth_codes (
    code VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_challenge VARCHAR(255) NOT NULL,
    code_challenge_method VARCHAR(50) NOT NULL,
    scopes JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE TABLE mcp_tokens (
    token_id VARCHAR(255) PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    principal_id VARCHAR(100) NOT NULL,
    principal_type VARCHAR(50) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    scopes JSONB NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);
