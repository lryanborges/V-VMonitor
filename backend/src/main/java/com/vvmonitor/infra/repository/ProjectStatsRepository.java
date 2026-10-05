package com.vvmonitor.infra.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Numeros exibidos nos cards de projeto, calculados para varios projetos numa unica consulta.
 * SQL nativo porque elements, tests e test_coverage ainda nao tem entidades JPA.
 * Conta tambem elementos em rascunho (DRAFT): o card mostra o tamanho do trabalho, submetido ou nao.
 */
@Repository
public class ProjectStatsRepository {

    private static final String SQL = """
            SELECT p.id,
                   (SELECT count(*) FROM elements e
                     WHERE e.project_id = p.id AND e.deleted_at IS NULL
                       AND e.kind IN ('FUNCTIONAL', 'NON_FUNCTIONAL')) AS requirements,
                   (SELECT count(*) FROM elements e
                     WHERE e.project_id = p.id AND e.deleted_at IS NULL
                       AND e.kind = 'BUSINESS_RULE') AS business_rules,
                   (SELECT count(*) FROM tests t
                     WHERE t.project_id = p.id AND t.deleted_at IS NULL) AS tests,
                   (SELECT count(DISTINCT e.id) FROM elements e
                      JOIN test_coverage tc ON tc.element_id = e.id AND tc.deleted_at IS NULL
                      JOIN tests t ON t.id = tc.test_id AND t.deleted_at IS NULL
                     WHERE e.project_id = p.id AND e.deleted_at IS NULL
                       AND e.kind IN ('FUNCTIONAL', 'NON_FUNCTIONAL')) AS requirements_with_tests,
                   (SELECT max(v.version_number) FROM project_versions v
                     WHERE v.project_id = p.id) AS latest_version
              FROM projects p
             WHERE p.id IN (:ids)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ProjectStatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<UUID, ProjectStatsView> findByProjectIds(Collection<UUID> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        List<ProjectStatsView> rows = jdbc.query(SQL, Map.of("ids", projectIds), (rs, i) -> new ProjectStatsView(
                rs.getObject("id", UUID.class),
                rs.getLong("requirements"),
                rs.getLong("business_rules"),
                rs.getLong("tests"),
                rs.getLong("requirements_with_tests"),
                rs.getObject("latest_version", Integer.class)));
        return rows.stream().collect(Collectors.toMap(ProjectStatsView::projectId, Function.identity()));
    }

    public record ProjectStatsView(UUID projectId, long requirements, long businessRules, long tests,
                                   long requirementsWithTests, Integer latestVersion) {

        public static ProjectStatsView empty(UUID projectId) {
            return new ProjectStatsView(projectId, 0, 0, 0, 0, null);
        }
    }
}
