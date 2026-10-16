-- V3: submeter o modelo passa a gerar uma versao imutavel (RF10 + RF17, RNF2).
-- O que esta pendente deixa de ser uma marca em cada item e passa a ser calculado comparando o modelo
-- atual com a ultima versao (project_versions.snapshot), o que inclui tambem as remocoes.
-- Projetos ja existentes nao tem versao: tudo neles aparece como adicionado ate a v1.

ALTER TABLE elements DROP COLUMN submission_status;
ALTER TABLE relationships DROP COLUMN submission_status;

-- a ultima submissao passa a ser a ultima versao (autor e data em project_versions)
ALTER TABLE projects
    DROP CONSTRAINT ck_projects_last_submission,
    DROP COLUMN last_submitted_at,
    DROP COLUMN last_submitted_by;
