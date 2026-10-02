-- V1: schema inicial do V&V Monitor (ver docs/schema.md)
-- Enums representados como varchar + CHECK, mapeados com @Enumerated(EnumType.STRING) no JPA.
-- Soft delete: registros com deleted_at preenchido sao considerados removidos;
-- unicidades usam indices parciais (WHERE deleted_at IS NULL).

-- =====================================================================
-- users (RF1, RF2, RNF3)
-- =====================================================================
CREATE TABLE users (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          varchar(120) NOT NULL,
    email         varchar(255) NOT NULL,
    password_hash varchar(72)  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted_at    timestamptz,
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE UNIQUE INDEX ux_users_email ON users (email) WHERE deleted_at IS NULL;

-- =====================================================================
-- projects (RF3, RF4)
-- =====================================================================
CREATE TABLE projects (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(150) NOT NULL,
    description text,
    owner_id    uuid         NOT NULL REFERENCES users (id),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted_at  timestamptz
);

CREATE INDEX ix_projects_owner ON projects (owner_id);

-- =====================================================================
-- project_members (RF20, RNF4)
-- =====================================================================
CREATE TABLE project_members (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid        NOT NULL REFERENCES projects (id),
    user_id    uuid        NOT NULL REFERENCES users (id),
    role       varchar(10) NOT NULL,
    added_at   timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_project_members_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

CREATE UNIQUE INDEX ux_project_members_project_user
    ON project_members (project_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_project_members_user ON project_members (user_id);

-- =====================================================================
-- elements: requisitos funcionais, nao funcionais e regras de negocio (RF5, RF6, RF10)
-- =====================================================================
CREATE TABLE elements (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        uuid        NOT NULL REFERENCES projects (id),
    kind              varchar(20) NOT NULL,
    code              varchar(20) NOT NULL,
    description       text        NOT NULL,
    priority          varchar(10),
    submission_status varchar(10) NOT NULL DEFAULT 'DRAFT',
    created_by        uuid        NOT NULL REFERENCES users (id),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    deleted_at        timestamptz,
    CONSTRAINT ck_elements_kind CHECK (kind IN ('FUNCTIONAL', 'NON_FUNCTIONAL', 'BUSINESS_RULE')),
    CONSTRAINT ck_elements_priority CHECK (priority IN ('MANDATORY', 'DESIRABLE', 'OPTIONAL')),
    CONSTRAINT ck_elements_submission_status CHECK (submission_status IN ('DRAFT', 'SUBMITTED')),
    -- requisitos exigem prioridade; regras de negocio nao possuem (RF5, RF6)
    CONSTRAINT ck_elements_priority_by_kind CHECK ((kind = 'BUSINESS_RULE') = (priority IS NULL))
);

CREATE UNIQUE INDEX ux_elements_project_code
    ON elements (project_id, code) WHERE deleted_at IS NULL;

-- =====================================================================
-- code_sequences: geracao dinamica de identificadores (RF1, RN1, T1...)
-- Codigos nunca sao reutilizados, mesmo apos remocao.
-- =====================================================================
CREATE TABLE code_sequences (
    project_id uuid        NOT NULL REFERENCES projects (id),
    kind       varchar(20) NOT NULL,
    next_value int         NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, kind),
    CONSTRAINT ck_code_sequences_kind CHECK (kind IN ('FUNCTIONAL', 'NON_FUNCTIONAL', 'BUSINESS_RULE', 'TEST')),
    CONSTRAINT ck_code_sequences_next_value CHECK (next_value >= 1)
);

-- =====================================================================
-- relationship_types (RF8); project_id nulo = tipo pre-definido global
-- =====================================================================
CREATE TABLE relationship_types (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid        REFERENCES projects (id),
    name       varchar(60) NOT NULL,
    is_symmetric boolean   NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

-- nomes comparados sem diferenciar maiusculas/minusculas
CREATE UNIQUE INDEX ux_relationship_types_project_name
    ON relationship_types (project_id, lower(name))
    WHERE project_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_relationship_types_global_name
    ON relationship_types (lower(name))
    WHERE project_id IS NULL AND deleted_at IS NULL;

INSERT INTO relationship_types (name, is_symmetric) VALUES
    ('Dependência',  false),
    ('Conflito',     true),
    ('Refinamento',  false),
    ('Similaridade', true);

-- =====================================================================
-- relationships (RF7, UC-06, RF10)
-- =====================================================================
CREATE TABLE relationships (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        uuid        NOT NULL REFERENCES projects (id),
    source_id         uuid        NOT NULL REFERENCES elements (id),
    target_id         uuid        NOT NULL REFERENCES elements (id),
    type_id           uuid        NOT NULL REFERENCES relationship_types (id),
    submission_status varchar(10) NOT NULL DEFAULT 'DRAFT',
    created_by        uuid        NOT NULL REFERENCES users (id),
    created_at        timestamptz NOT NULL DEFAULT now(),
    deleted_at        timestamptz,
    CONSTRAINT ck_relationships_not_self CHECK (source_id <> target_id),
    CONSTRAINT ck_relationships_submission_status CHECK (submission_status IN ('DRAFT', 'SUBMITTED'))
);

-- impede relacionamento duplicado (excecao do UC-06)
CREATE UNIQUE INDEX ux_relationships_source_target_type
    ON relationships (source_id, target_id, type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_relationships_project ON relationships (project_id);
CREATE INDEX ix_relationships_target ON relationships (target_id);

-- =====================================================================
-- tests (RF9)
-- =====================================================================
CREATE TABLE tests (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  uuid        NOT NULL REFERENCES projects (id),
    code        varchar(20) NOT NULL,
    description text        NOT NULL,
    status      varchar(10) NOT NULL DEFAULT 'NOT_RUN',
    created_by  uuid        NOT NULL REFERENCES users (id),
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    deleted_at  timestamptz,
    CONSTRAINT ck_tests_status CHECK (status IN ('NOT_RUN', 'PASSED', 'FAILED'))
);

CREATE UNIQUE INDEX ux_tests_project_code
    ON tests (project_id, code) WHERE deleted_at IS NULL;

-- =====================================================================
-- test_coverage (RF9, RF13); element_id restrito a requisitos na aplicacao
-- =====================================================================
CREATE TABLE test_coverage (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id    uuid        NOT NULL REFERENCES tests (id),
    element_id uuid        NOT NULL REFERENCES elements (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX ux_test_coverage_test_element
    ON test_coverage (test_id, element_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_test_coverage_element ON test_coverage (element_id);

-- =====================================================================
-- project_versions (RF17, RF18, RNF2): imutavel, sem soft delete
-- =====================================================================
CREATE TABLE project_versions (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     uuid         NOT NULL REFERENCES projects (id),
    version_number int          NOT NULL,
    label          varchar(100),
    snapshot       jsonb        NOT NULL,
    created_by     uuid         NOT NULL REFERENCES users (id),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ux_project_versions_project_number UNIQUE (project_id, version_number),
    CONSTRAINT ck_project_versions_number CHECK (version_number >= 1)
);

CREATE FUNCTION prevent_project_version_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'project_versions e imutavel (RNF2): % nao permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_project_versions_immutable
    BEFORE UPDATE OR DELETE ON project_versions
    FOR EACH ROW EXECUTE FUNCTION prevent_project_version_change();
