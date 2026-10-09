package com.vvmonitor.api.dto.response;

import java.time.Instant;

/** RF10: quantos elementos e relacoes passaram de rascunho para submetidos, quando e por quem. */
public record SubmissionResponse(
        int elements,
        int relationships,
        Instant submittedAt,
        ProjectResponse.PersonSummary submittedBy
) {
}
