CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(100),
    phone_number VARCHAR(30)
);

ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30);

CREATE TABLE IF NOT EXISTS user_health_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    has_asthma BOOLEAN DEFAULT false,
    has_allergies BOOLEAN DEFAULT false,
    has_skin_sensitivity BOOLEAN DEFAULT false,
    has_migraines BOOLEAN DEFAULT false,
    has_joint_pain BOOLEAN DEFAULT false,
    has_heat_sensitivity BOOLEAN DEFAULT false,
    aqi_threshold INT DEFAULT 100,
    uv_threshold INT DEFAULT 6,
    humidity_threshold INT DEFAULT 70,
    pollen_threshold VARCHAR(20) DEFAULT 'MODERATE',
    alerts_enabled BOOLEAN DEFAULT true,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
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

CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(255),
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alert_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    fcm_token VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS alert_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE IF EXISTS alert_log ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE IF EXISTS alert_log ADD COLUMN IF NOT EXISTS headline VARCHAR(255);
ALTER TABLE IF EXISTS alert_log ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE IF EXISTS alert_log ADD COLUMN IF NOT EXISTS severity VARCHAR(20) DEFAULT 'WARNING';
ALTER TABLE IF EXISTS alert_log ADD COLUMN IF NOT EXISTS latitude NUMERIC(9, 6);
ALTER TABLE IF EXISTS alert_log ADD COLUMN IF NOT EXISTS longitude NUMERIC(9, 6);

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    widget_order TEXT,
    disabled_widgets TEXT,
    default_screen VARCHAR(50) DEFAULT 'home',
    temperature_unit VARCHAR(10) DEFAULT 'CELSIUS',
    school_start VARCHAR(10) DEFAULT '08:00',
    school_end VARCHAR(10) DEFAULT '15:00',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_location_history (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    latitude NUMERIC(8, 4) NOT NULL,
    longitude NUMERIC(8, 4) NOT NULL,
    accuracy_meters REAL,
    hour_of_day SMALLINT NOT NULL,
    day_of_week SMALLINT NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_location_history_user_time ON user_location_history (user_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_location_routine ON user_location_history (user_id, day_of_week, hour_of_day);
