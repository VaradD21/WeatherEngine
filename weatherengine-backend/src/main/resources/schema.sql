CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS personas (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_personas (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    persona_id INT REFERENCES personas(id),
    PRIMARY KEY (user_id, persona_id)
);

CREATE TABLE IF NOT EXISTS persona_widgets (
    id SERIAL PRIMARY KEY,
    persona_id INT REFERENCES personas(id),
    widget_code VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    UNIQUE (persona_id, widget_code)
);
