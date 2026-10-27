package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;

import java.util.List;
import java.util.UUID;

/** RF18: uma versao com o modelo completo daquele momento (so leitura). */
public record VersionResponse(
        VersionSummaryResponse version,
        List<ElementSnapshot> elements,
        List<VersionedRelationship> relationships,
        List<VersionedTest> tests
) {

    /** Relacao com as pontas e o tipo como estavam na versao. */
    public record VersionedRelationship(UUID id, RelationshipTypeResponse type, ElementSummary source,
                                        ElementSummary target) {
    }

    /** Teste com os requisitos que cobria na versao. */
    public record VersionedTest(UUID id, String code, String description, List<ElementSummary> covers) {
    }

    public static VersionResponse from(VersionSummaryResponse version, ModelSnapshot snapshot) {
        SnapshotIndex index = new SnapshotIndex(snapshot);
        return new VersionResponse(version, snapshot.elements(),
                snapshot.relationships().stream().map(index::relationship).toList(),
                snapshot.tests().stream().map(index::test).toList());
    }
}
