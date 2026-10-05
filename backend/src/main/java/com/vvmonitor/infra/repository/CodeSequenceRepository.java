package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.enums.SequenceKind;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

/**
 * Contadores de codigo por projeto e tipo (RF1, RNF1, RN1, T1...). Os numeros nunca sao reutilizados,
 * mesmo apos a remocao do elemento.
 */
@Repository
public class CodeSequenceRepository {

    /**
     * Reserva o proximo numero numa unica instrucao: cria o contador na primeira vez e incrementa nas
     * seguintes. O ON CONFLICT trava a linha, entao dois cadastros simultaneos nunca recebem o mesmo numero.
     */
    private static final String ALLOCATE = """
            INSERT INTO code_sequences (project_id, kind, next_value)
            VALUES (:projectId, :kind, 2)
            ON CONFLICT (project_id, kind)
            DO UPDATE SET next_value = code_sequences.next_value + 1
            RETURNING next_value - 1
            """;

    private static final String PEEK = """
            SELECT coalesce(max(next_value), 1) FROM code_sequences
             WHERE project_id = :projectId AND kind = :kind
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CodeSequenceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Reserva e devolve o proximo numero; deve rodar na mesma transacao que salva o elemento. */
    public int allocate(UUID projectId, SequenceKind kind) {
        return jdbc.queryForObject(ALLOCATE, params(projectId, kind), Integer.class);
    }

    /** Proximo numero sem reservar (previa exibida no formulario). */
    public int peek(UUID projectId, SequenceKind kind) {
        return jdbc.queryForObject(PEEK, params(projectId, kind), Integer.class);
    }

    private static Map<String, Object> params(UUID projectId, SequenceKind kind) {
        return Map.of("projectId", projectId, "kind", kind.name());
    }
}
