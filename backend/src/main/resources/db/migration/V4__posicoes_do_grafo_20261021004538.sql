-- V4: posicao dos nos na visualizacao em grafo (RF21, proposto para a v2 do documento; RF11).
-- Compartilhada entre os membros do projeto; salva ao soltar um no arrastado (so proprietario e editores).
-- node_id e o id de um elemento ou de um teste do projeto (sem FK: pode apontar para qualquer dos dois).
-- Dado de apresentacao, fora das versoes: sem soft delete, a linha e apagada quando o no deixa de existir.

CREATE TABLE graph_positions (
    project_id uuid             NOT NULL REFERENCES projects (id),
    node_id    uuid             NOT NULL,
    x          double precision NOT NULL,
    y          double precision NOT NULL,
    updated_by uuid             NOT NULL REFERENCES users (id),
    updated_at timestamptz      NOT NULL DEFAULT now(),
    CONSTRAINT pk_graph_positions PRIMARY KEY (project_id, node_id)
);
