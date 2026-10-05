package com.vvmonitor.infra.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consultas e alteracoes sobre testes e seus vinculos usadas na exclusao de elementos (RF15, RF16).
 * SQL nativo porque tests e test_coverage ainda nao tem entidades JPA (chegam na etapa de testes, RF9).
 */
@Repository
public class TestCoverageQueryRepository {

    /** Testes que cobrem o elemento e nenhum outro requisito ativo: ficam sem requisito se ele for excluido. */
    private static final String ONLY_COVERING = """
            SELECT t.id, t.code, t.description
              FROM tests t
              JOIN test_coverage tc ON tc.test_id = t.id AND tc.deleted_at IS NULL
             WHERE t.deleted_at IS NULL AND tc.element_id = :elementId
               AND NOT EXISTS (
                   SELECT 1 FROM test_coverage o
                     JOIN elements e ON e.id = o.element_id AND e.deleted_at IS NULL
                    WHERE o.test_id = t.id AND o.deleted_at IS NULL AND o.element_id <> :elementId)
             ORDER BY t.code
            """;

    private static final String SOFT_DELETE_OF_ELEMENT = """
            UPDATE test_coverage SET deleted_at = now()
             WHERE deleted_at IS NULL AND element_id = :elementId
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public TestCoverageQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TestSummaryView> testsOnlyCovering(UUID elementId) {
        return jdbc.query(ONLY_COVERING, Map.of("elementId", elementId), (rs, i) -> new TestSummaryView(
                rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("description")));
    }

    /** RF15: remove (soft delete) os vinculos teste-requisito de um elemento que esta sendo excluido. */
    public int softDeleteCoverageOf(UUID elementId) {
        return jdbc.update(SOFT_DELETE_OF_ELEMENT, Map.of("elementId", elementId));
    }

    public record TestSummaryView(UUID id, String code, String description) {
    }
}
