package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.version.ChangeSummary;
import com.vvmonitor.infra.repository.ProjectVersionRepository.VersionRow;

import java.time.Instant;
import java.util.UUID;

/**
 * Versao na lista (RF18): numero, nome opcional, autor, data, totais do modelo naquele momento e o resumo do
 * que mudou em relacao a versao anterior.
 */
public record VersionSummaryResponse(
        UUID id,
        int number,
        String label,
        ProjectResponse.PersonSummary createdBy,
        Instant createdAt,
        int elements,
        int relationships,
        ChangeSummary changes
) {

    public static VersionSummaryResponse from(VersionRow row) {
        return new VersionSummaryResponse(row.id(), row.number(), row.label(),
                new ProjectResponse.PersonSummary(row.createdBy(), row.createdByName()), row.createdAt(),
                row.elements(), row.relationships(), row.changes());
    }
}
