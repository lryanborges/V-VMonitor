package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.version.ChangeSummary;
import com.vvmonitor.domain.version.ModelSnapshot;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Versoes do modelo (RF17, RF18), gravadas em project_versions. A tabela e imutavel (RNF2): so ha insercao,
 * e um trigger do banco recusa UPDATE e DELETE. SQL nativo porque o conteudo e jsonb.
 */
@Repository
public class ProjectVersionRepository {

    /** Colunas da lista: o conteudo completo nao e lido, so os totais e o resumo das mudancas. */
    private static final String SUMMARY_COLUMNS = """
            v.id, v.version_number, v.label, v.created_by, u.name AS created_by_name, v.created_at,
            jsonb_array_length(v.snapshot -> 'elements') AS elements,
            jsonb_array_length(v.snapshot -> 'relationships') AS relationships,
            COALESCE(jsonb_array_length(v.snapshot -> 'tests'), 0) AS tests,
            v.snapshot -> 'changes' AS changes
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;

    public ProjectVersionRepository(NamedParameterJdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Dados de uma versao sem o conteudo completo. createdByName e null se o autor nao existir mais. */
    public record VersionRow(UUID id, int number, String label, UUID createdBy, String createdByName,
                             Instant createdAt, int elements, int relationships, int tests, ChangeSummary changes) {
    }

    /**
     * Trava o projeto ate o fim da transacao, para que duas submissoes simultaneas nao calculem o mesmo
     * numero de versao (a restricao unica (project_id, version_number) e a garantia final).
     */
    public void lockProject(UUID projectId) {
        jdbc.queryForList("SELECT id FROM projects WHERE id = :id FOR UPDATE", Map.of("id", projectId));
    }

    /** Mais recentes primeiro. */
    public List<VersionRow> findAll(UUID projectId) {
        return jdbc.query("SELECT " + SUMMARY_COLUMNS + """
                  FROM project_versions v LEFT JOIN users u ON u.id = v.created_by
                 WHERE v.project_id = :projectId
                 ORDER BY v.version_number DESC
                """, Map.of("projectId", projectId), this::toRow);
    }

    public Optional<VersionRow> find(UUID projectId, int number) {
        return jdbc.query("SELECT " + SUMMARY_COLUMNS + """
                  FROM project_versions v LEFT JOIN users u ON u.id = v.created_by
                 WHERE v.project_id = :projectId AND v.version_number = :number
                """, Map.of("projectId", projectId, "number", number), this::toRow).stream().findFirst();
    }

    /** Conteudo completo de uma versao. */
    public Optional<ModelSnapshot> findSnapshot(UUID projectId, int number) {
        return jdbc.query("""
                SELECT snapshot FROM project_versions WHERE project_id = :projectId AND version_number = :number
                """, Map.of("projectId", projectId, "number", number),
                (rs, i) -> json.readValue(rs.getString("snapshot"), ModelSnapshot.class)).stream().findFirst();
    }

    /** Numero e conteudo da ultima versao do projeto; vazio antes da primeira. */
    public Optional<Latest> findLatest(UUID projectId) {
        return jdbc.query("""
                SELECT version_number, snapshot FROM project_versions
                 WHERE project_id = :projectId ORDER BY version_number DESC LIMIT 1
                """, Map.of("projectId", projectId),
                (rs, i) -> new Latest(rs.getInt("version_number"),
                        json.readValue(rs.getString("snapshot"), ModelSnapshot.class))).stream().findFirst();
    }

    public record Latest(int number, ModelSnapshot snapshot) {
    }

    /** Grava uma nova versao (nunca altera as existentes) e devolve o id gerado. */
    public UUID insert(UUID projectId, int number, String label, ModelSnapshot snapshot, UUID createdBy,
                       Instant createdAt) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("number", number);
        params.put("label", label);
        params.put("snapshot", json.writeValueAsString(snapshot));
        params.put("createdBy", createdBy);
        params.put("createdAt", Timestamp.from(createdAt));
        return jdbc.queryForObject("""
                INSERT INTO project_versions (project_id, version_number, label, snapshot, created_by, created_at)
                VALUES (:projectId, :number, :label, CAST(:snapshot AS jsonb), :createdBy, :createdAt)
                RETURNING id
                """, params, UUID.class);
    }

    private VersionRow toRow(ResultSet rs, int i) throws SQLException {
        String changes = rs.getString("changes");
        return new VersionRow(
                rs.getObject("id", UUID.class),
                rs.getInt("version_number"),
                rs.getString("label"),
                rs.getObject("created_by", UUID.class),
                rs.getString("created_by_name"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getInt("elements"),
                rs.getInt("relationships"),
                rs.getInt("tests"),
                changes == null ? null : json.readValue(changes, ChangeSummary.class));
    }
}
