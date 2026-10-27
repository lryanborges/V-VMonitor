package com.vvmonitor.infra.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Posicao dos nos no grafo (RF21), por projeto. SQL nativo: upsert em lote e chave composta. */
@Repository
public class GraphPositionRepository {

    /** So nos que ainda existem: elementos e testes ativos do projeto. */
    private static final String FIND = """
            SELECT g.node_id, g.x, g.y
              FROM graph_positions g
             WHERE g.project_id = :projectId
               AND (EXISTS (SELECT 1 FROM elements e
                             WHERE e.id = g.node_id AND e.project_id = g.project_id AND e.deleted_at IS NULL)
                 OR EXISTS (SELECT 1 FROM tests t
                             WHERE t.id = g.node_id AND t.project_id = g.project_id AND t.deleted_at IS NULL))
            """;

    private static final String UPSERT = """
            INSERT INTO graph_positions (project_id, node_id, x, y, updated_by, updated_at)
            VALUES (:projectId, :nodeId, :x, :y, :userId, now())
            ON CONFLICT (project_id, node_id) DO UPDATE
               SET x = EXCLUDED.x, y = EXCLUDED.y, updated_by = EXCLUDED.updated_by, updated_at = now()
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public GraphPositionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Position(UUID nodeId, double x, double y) {
    }

    public List<Position> findByProject(UUID projectId) {
        return jdbc.query(FIND, Map.of("projectId", projectId), (rs, i) ->
                new Position(rs.getObject("node_id", UUID.class), rs.getDouble("x"), rs.getDouble("y")));
    }

    public void saveAll(UUID projectId, UUID userId, Collection<Position> positions) {
        MapSqlParameterSource[] batch = positions.stream()
                .map(p -> new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("nodeId", p.nodeId())
                        .addValue("x", p.x())
                        .addValue("y", p.y())
                        .addValue("userId", userId))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate(UPSERT, batch);
    }

    /** O no deixou de existir (elemento ou teste excluido): a posicao sai junto. */
    public void deleteNodes(Collection<UUID> nodeIds) {
        if (!nodeIds.isEmpty()) {
            jdbc.update("DELETE FROM graph_positions WHERE node_id IN (:ids)", Map.of("ids", nodeIds));
        }
    }
}
