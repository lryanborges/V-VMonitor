package com.vvmonitor.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * RF16: o que muda no modelo se o elemento for excluido, para o alerta antes da confirmacao.
 *
 * @param level                       LOW, MEDIUM ou HIGH (regra em ElementService.impactLevel)
 * @param relationships               relacoes que serao removidas junto com o elemento
 * @param orphans                     elementos cujo unico vinculo e com o excluido (ficam sem vinculo algum)
 * @param dependents                  elementos que dependem do excluido (perdem uma dependencia)
 * @param testsLeftWithoutRequirement testes que so cobrem o excluido (ficam sem requisito)
 * @param tests                       quantidade de testes associados ao elemento
 */
public record DeleteImpactResponse(
        ImpactLevel level,
        List<ImpactRelationship> relationships,
        List<ElementSummary> orphans,
        List<ElementSummary> dependents,
        List<TestSummary> testsLeftWithoutRequirement,
        long tests
) {

    public enum ImpactLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    /** outgoing: o elemento excluido e a origem da relacao (ex.: "RF7 depende de RF5"). */
    public record ImpactRelationship(UUID id, String typeName, boolean outgoing, ElementSummary other) {
    }

    public record TestSummary(UUID id, String code, String description) {
    }
}
