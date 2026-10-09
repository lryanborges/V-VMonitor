-- V2: registro da ultima submissao do modelo (RF10).
-- O historico completo de cada estado submetido fica para as versoes (RF17, project_versions).

ALTER TABLE projects
    ADD COLUMN last_submitted_at timestamptz,
    ADD COLUMN last_submitted_by uuid REFERENCES users (id);

-- quem submeteu e quando andam juntos: os dois preenchidos ou os dois vazios
ALTER TABLE projects
    ADD CONSTRAINT ck_projects_last_submission
        CHECK ((last_submitted_at IS NULL) = (last_submitted_by IS NULL));
