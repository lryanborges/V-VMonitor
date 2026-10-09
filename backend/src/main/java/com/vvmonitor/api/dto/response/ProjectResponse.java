package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.enums.MemberRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Projeto como exibido no card da tela de projetos e no cabecalho do projeto.
 * role e a permissao do usuario logado; projeto compartilhado = role diferente de OWNER.
 */
public record ProjectResponse(
        UUID id,
        String name,
        String description,
        MemberRole role,
        PersonSummary owner,
        List<MemberSummary> members,
        Stats stats,
        Integer latestVersion,
        /** RF10: ultima submissao do modelo; nulos ate a primeira. */
        Instant lastSubmittedAt,
        PersonSummary lastSubmittedBy,
        Instant createdAt,
        Instant updatedAt
) {

    public record PersonSummary(UUID id, String name) {
    }

    public record MemberSummary(UUID id, String name, MemberRole role) {
    }

    /** coverage: percentual (0 a 100) de requisitos com ao menos um teste; regras de negocio nao entram. */
    public record Stats(long requirements, long businessRules, long tests,
                        long requirementsWithTests, int coverage) {

        public static Stats of(long requirements, long businessRules, long tests, long requirementsWithTests) {
            int coverage = requirements == 0 ? 0 : (int) Math.round(100.0 * requirementsWithTests / requirements);
            return new Stats(requirements, businessRules, tests, requirementsWithTests, coverage);
        }
    }
}
