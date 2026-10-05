package com.vvmonitor.infra.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Colunas "Vinculos" e "Testes" da lista de elementos, para todos os elementos de um projeto numa
 * unica consulta. SQL nativo porque relationships e test_coverage ainda nao tem entidades JPA.
 */
@Repository
public class ElementStatsRepository {

    private static final String SQL = """
            SELECT e.id,
                   (SELECT count(*) FROM relationships r
                     WHERE r.deleted_at IS NULL
                       AND (r.source_id = e.id OR r.target_id = e.id)) AS links,
                   (SELECT count(*) FROM test_coverage tc
                      JOIN tests t ON t.id = tc.test_id AND t.deleted_at IS NULL
                     WHERE tc.element_id = e.id AND tc.deleted_at IS NULL) AS tests
              FROM elements e
             WHERE e.project_id = :projectId AND e.deleted_at IS NULL
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ElementStatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<UUID, ElementStats> findByProjectId(UUID projectId) {
        Map<UUID, ElementStats> stats = new HashMap<>();
        jdbc.query(SQL, Map.of("projectId", projectId), rs -> {
            stats.put(rs.getObject("id", UUID.class), new ElementStats(rs.getLong("links"), rs.getLong("tests")));
        });
        return stats;
    }

    public record ElementStats(long links, long tests) {

        public static final ElementStats NONE = new ElementStats(0, 0);
    }
}
