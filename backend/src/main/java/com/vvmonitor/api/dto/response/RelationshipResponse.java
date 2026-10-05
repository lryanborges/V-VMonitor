package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

/** Relacao com os dados minimos das duas pontas, para exibir sem consultar os elementos de novo. */
public record RelationshipResponse(
        UUID id,
        RelationshipTypeResponse type,
        ElementSummary source,
        ElementSummary target,
        SubmissionStatus submissionStatus,
        Instant createdAt
) {

    public static RelationshipResponse from(Relationship relationship, RelationshipType type, Element source,
                                            Element target) {
        return new RelationshipResponse(relationship.getId(), RelationshipTypeResponse.from(type),
                ElementSummary.from(source), ElementSummary.from(target), relationship.getSubmissionStatus(),
                relationship.getCreatedAt());
    }
}
