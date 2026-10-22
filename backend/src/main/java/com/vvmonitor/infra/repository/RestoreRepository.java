package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.enums.Priority;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Operacoes da restauracao de versao (RF22, proposto para a v2 do documento). SQL nativo porque restaurar
 * precisa enxergar e "reviver" registros com exclusao logica, que as entidades JPA escondem (@SQLRestriction).
 * Todas as operacoes sao restritas ao projeto informado.
 */
@Repository
public class RestoreRepository {

    /** Unicas tabelas aceitas nos metodos genericos (o nome entra no SQL). */
    private static final List<String> TABLES = List.of("elements", "relationships", "tests");

    private final NamedParameterJdbcTemplate jdbc;

    public RestoreRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ids ativos (nao excluidos) de uma tabela do projeto: elements, relationships ou tests. */
    public List<UUID> activeIds(String table, UUID projectId) {
        checkTable(table);
        return jdbc.queryForList("SELECT id FROM " + table + " WHERE project_id = :p AND deleted_at IS NULL",
                Map.of("p", projectId), UUID.class);
    }

    /** Exclusao logica, como a exclusao normal; para elementos, leva junto relacoes e vinculos com testes. */
    public void softDelete(String table, UUID projectId, Collection<UUID> ids) {
        checkTable(table);
        if (ids.isEmpty()) {
            return;
        }
        Map<String, Object> params = Map.of("p", projectId, "ids", ids);
        if (table.equals("elements")) {
            jdbc.update("""
                    UPDATE relationships SET deleted_at = now()
                     WHERE project_id = :p AND deleted_at IS NULL AND (source_id IN (:ids) OR target_id IN (:ids))
                    """, params);
            jdbc.update("UPDATE test_coverage SET deleted_at = now() WHERE deleted_at IS NULL AND element_id IN (:ids)",
                    Map.of("ids", ids));
        }
        if (table.equals("tests")) {
            jdbc.update("UPDATE test_coverage SET deleted_at = now() WHERE deleted_at IS NULL AND test_id IN (:ids)",
                    Map.of("ids", ids));
        }
        jdbc.update("UPDATE " + table + " SET deleted_at = now() WHERE project_id = :p AND deleted_at IS NULL AND id IN (:ids)",
                params);
        jdbc.update("DELETE FROM graph_positions WHERE project_id = :p AND node_id IN (:ids)", params);
    }

    private static void checkTable(String table) {
        if (!TABLES.contains(table)) {
            throw new IllegalArgumentException("Tabela não permitida: " + table);
        }
    }

    /** Volta o elemento (mesmo id e codigo) ao conteudo da versao; reativa se estava excluido. */
    public void restoreElement(UUID projectId, UUID id, String description, Priority priority) {
        jdbc.update("""
                UPDATE elements SET deleted_at = NULL, description = :d, priority = :pr, updated_at = now()
                 WHERE id = :id AND project_id = :p
                """, new MapSqlParameterSource().addValue("id", id).addValue("p", projectId).addValue("d", description)
                .addValue("pr", priority == null ? null : priority.name()));
    }

    /** Reativa tipos personalizados usados pela versao (os pre-definidos nunca sao excluidos). */
    public void restoreTypes(UUID projectId, Collection<UUID> typeIds) {
        if (!typeIds.isEmpty()) {
            jdbc.update("UPDATE relationship_types SET deleted_at = NULL WHERE project_id = :p AND id IN (:ids)",
                    Map.of("p", projectId, "ids", typeIds));
        }
    }

    public void restoreRelationship(UUID projectId, UUID id, UUID sourceId, UUID targetId, UUID typeId) {
        jdbc.update("""
                UPDATE relationships SET deleted_at = NULL, source_id = :s, target_id = :t, type_id = :ty
                 WHERE id = :id AND project_id = :p
                """, Map.of("id", id, "p", projectId, "s", sourceId, "t", targetId, "ty", typeId));
    }

    public void restoreTest(UUID projectId, UUID id, String description) {
        jdbc.update("""
                UPDATE tests SET deleted_at = NULL, description = :d, updated_at = now()
                 WHERE id = :id AND project_id = :p
                """, Map.of("id", id, "p", projectId, "d", description));
    }

    /**
     * Deixa a cobertura do teste igual a da versao: remove os vinculos que sobram e garante os que faltam
     * (reativando o vinculo antigo, se existir, ou criando um novo).
     */
    public void restoreCoverage(UUID testId, Collection<UUID> elementIds) {
        if (elementIds.isEmpty()) {
            jdbc.update("UPDATE test_coverage SET deleted_at = now() WHERE deleted_at IS NULL AND test_id = :t",
                    Map.of("t", testId));
            return;
        }
        jdbc.update("""
                UPDATE test_coverage SET deleted_at = now()
                 WHERE deleted_at IS NULL AND test_id = :t AND element_id NOT IN (:ids)
                """, Map.of("t", testId, "ids", elementIds));
        for (UUID elementId : elementIds) {
            Map<String, Object> pair = Map.of("t", testId, "e", elementId);
            int revived = jdbc.update("""
                    UPDATE test_coverage SET deleted_at = NULL
                     WHERE id = (SELECT id FROM test_coverage WHERE test_id = :t AND element_id = :e
                                 ORDER BY deleted_at IS NULL DESC, created_at DESC LIMIT 1)
                    """, pair);
            if (revived == 0) {
                jdbc.update("INSERT INTO test_coverage (test_id, element_id) VALUES (:t, :e)", pair);
            }
        }
    }
}
