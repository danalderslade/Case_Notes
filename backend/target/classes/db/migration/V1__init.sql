-- ============================================================
-- Case Management Notes Module - PostgreSQL Schema
-- Supports per-country tables using ISO 3166-1 alpha-2 codes
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ISO 3166-1 Country Reference Table
-- ============================================================
CREATE TABLE IF NOT EXISTS ref_countries (
    iso_alpha2   CHAR(2)      PRIMARY KEY,
    iso_alpha3   CHAR(3)      NOT NULL UNIQUE,
    iso_numeric  CHAR(3)      NOT NULL UNIQUE,
    country_name VARCHAR(100) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Seed common countries (extend as needed)
INSERT INTO ref_countries (iso_alpha2, iso_alpha3, iso_numeric, country_name) VALUES
    ('GB', 'GBR', '826', 'United Kingdom'),
    ('US', 'USA', '840', 'United States of America'),
    ('AU', 'AUS', '036', 'Australia'),
    ('CA', 'CAN', '124', 'Canada'),
    ('DE', 'DEU', '276', 'Germany'),
    ('FR', 'FRA', '250', 'France'),
    ('IE', 'IRL', '372', 'Ireland'),
    ('NZ', 'NZL', '554', 'New Zealand'),
    ('ZA', 'ZAF', '710', 'South Africa'),
    ('IN', 'IND', '356', 'India')
ON CONFLICT (iso_alpha2) DO NOTHING;

-- ============================================================
-- Template function: Creates a country-partitioned notes table
-- Usage: SELECT create_country_notes_table('GB');
-- ============================================================
CREATE OR REPLACE FUNCTION create_country_notes_table(p_country_code CHAR(2))
RETURNS VOID AS $$
DECLARE
    v_table_name TEXT;
    v_seq_name   TEXT;
    v_idx_prefix TEXT;
BEGIN
    -- Validate country code exists
    IF NOT EXISTS (SELECT 1 FROM ref_countries WHERE iso_alpha2 = UPPER(p_country_code)) THEN
        RAISE EXCEPTION 'Country code % not found in ref_countries', p_country_code;
    END IF;

    v_table_name := 'case_notes_' || LOWER(p_country_code);
    v_idx_prefix := 'idx_' || LOWER(p_country_code) || '_notes';

    -- Create the notes table for this country
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I (
            id              UUID         NOT NULL DEFAULT uuid_generate_v4(),
            case_id         VARCHAR(100) NOT NULL,
            staff_id        VARCHAR(100) NOT NULL,
            country_code    CHAR(2)      NOT NULL DEFAULT %L,
            note_content    TEXT         NOT NULL,  -- Stored as HTML/rich-text (TipTap output)
            note_plain_text TEXT,                    -- Stripped plain-text for search
            is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
            deleted_at      TIMESTAMPTZ,
            deleted_by      VARCHAR(100),
            delete_reason   VARCHAR(500),
            created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
            version         INTEGER      NOT NULL DEFAULT 1,  -- Optimistic locking
            CONSTRAINT %I PRIMARY KEY (id),
            CONSTRAINT %I CHECK (country_code = %L),
            CONSTRAINT %I FOREIGN KEY (country_code) REFERENCES ref_countries(iso_alpha2)
        )',
        v_table_name,
        UPPER(p_country_code),
        v_table_name || '_pkey',
        v_table_name || '_country_chk',
        v_table_name || '_country_fk'
    );

    -- Indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (case_id) WHERE is_deleted = FALSE',
        v_idx_prefix || '_case_id', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (staff_id) WHERE is_deleted = FALSE',
        v_idx_prefix || '_staff_id', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (created_at DESC)',
        v_idx_prefix || '_created_at', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (is_deleted)',
        v_idx_prefix || '_is_deleted', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I USING GIN (to_tsvector(''english'', COALESCE(note_plain_text, '''' )))',
        v_idx_prefix || '_fts', v_table_name);

    -- Auto-update updated_at trigger
    EXECUTE format('
        CREATE OR REPLACE TRIGGER trg_%s_updated_at
        BEFORE UPDATE ON %I
        FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at()',
        LOWER(p_country_code) || '_notes',
        v_table_name
    );

    RAISE NOTICE 'Created notes table: %', v_table_name;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Shared trigger function for updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version    = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Audit log table (shared across all countries)
-- ============================================================
CREATE TABLE IF NOT EXISTS case_notes_audit (
    audit_id      UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    country_code  CHAR(2)     NOT NULL REFERENCES ref_countries(iso_alpha2),
    note_id       UUID        NOT NULL,
    case_id       VARCHAR(100) NOT NULL,
    staff_id      VARCHAR(100) NOT NULL,
    action        VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'SOFT_DELETE', 'RESTORE')),
    changed_by    VARCHAR(100) NOT NULL,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    old_values    JSONB,
    new_values    JSONB,
    ip_address    INET,
    user_agent    TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_note_id     ON case_notes_audit (note_id);
CREATE INDEX IF NOT EXISTS idx_audit_case_id     ON case_notes_audit (case_id);
CREATE INDEX IF NOT EXISTS idx_audit_changed_at  ON case_notes_audit (changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_country     ON case_notes_audit (country_code);

-- ============================================================
-- Create default tables for seeded countries
-- ============================================================
SELECT create_country_notes_table('GB');
SELECT create_country_notes_table('US');
SELECT create_country_notes_table('AU');
SELECT create_country_notes_table('CA');
SELECT create_country_notes_table('IE');
