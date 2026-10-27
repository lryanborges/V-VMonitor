package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.entity.TestCase;
import com.vvmonitor.domain.enums.SubmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Teste com os requisitos que cobre (RF9). covers vazio = teste que ficou sem requisito (o requisito foi
 * excluido). submissionStatus como nos elementos: DRAFT se mudou desde a ultima versao.
 */
public record TestResponse(
        UUID id,
        String code,
        String description,
        List<ElementSummary> covers,
        SubmissionStatus submissionStatus,
        Instant createdAt,
        Instant updatedAt
) {

    public static TestResponse from(TestCase test, List<ElementSummary> covers, boolean pending) {
        return new TestResponse(test.getId(), test.getCode(), test.getDescription(), covers,
                SubmissionStatus.of(pending), test.getCreatedAt(), test.getUpdatedAt());
    }
}
