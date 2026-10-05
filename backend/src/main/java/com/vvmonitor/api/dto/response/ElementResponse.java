package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.infra.repository.ElementStatsRepository.ElementStats;

import java.time.Instant;
import java.util.UUID;

/** Linha da lista do modelo: links e tests alimentam as colunas "Vinculos" e "Testes". */
public record ElementResponse(
        UUID id,
        String code,
        ElementKind kind,
        String description,
        Priority priority,
        SubmissionStatus submissionStatus,
        long links,
        long tests,
        Instant createdAt,
        Instant updatedAt
) {

    public static ElementResponse from(Element element, ElementStats stats) {
        return new ElementResponse(element.getId(), element.getCode(), element.getKind(), element.getDescription(),
                element.getPriority(), element.getSubmissionStatus(), stats.links(), stats.tests(),
                element.getCreatedAt(), element.getUpdatedAt());
    }
}
