package com.vvmonitor.api.dto.response;

import com.vvmonitor.api.dto.response.VersionResponse.VersionedRelationship;
import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.RelationshipSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TypeSnapshot;

import java.util.Map;
import java.util.UUID;

/** Elementos e tipos de uma copia do modelo indexados por id, para resolver as pontas e o tipo das relacoes. */
final class SnapshotIndex {

    private final Map<UUID, ElementSnapshot> elements;
    private final Map<UUID, TypeSnapshot> types;

    SnapshotIndex(ModelSnapshot snapshot) {
        this.elements = snapshot.elementsById();
        this.types = snapshot.typesById();
    }

    VersionedRelationship relationship(RelationshipSnapshot r) {
        TypeSnapshot type = types.get(r.typeId());
        return new VersionedRelationship(r.id(),
                new RelationshipTypeResponse(type.id(), type.name(), type.symmetric(), type.custom()),
                summary(elements.get(r.sourceId())), summary(elements.get(r.targetId())));
    }

    private static ElementSummary summary(ElementSnapshot element) {
        return new ElementSummary(element.id(), element.code(), element.kind(), element.description());
    }
}
