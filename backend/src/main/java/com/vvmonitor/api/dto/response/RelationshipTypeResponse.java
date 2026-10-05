package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.entity.RelationshipType;

import java.util.UUID;

/** custom: criado pelo ator no projeto; false para os quatro pre-definidos. */
public record RelationshipTypeResponse(UUID id, String name, boolean symmetric, boolean custom) {

    public static RelationshipTypeResponse from(RelationshipType type) {
        return new RelationshipTypeResponse(type.getId(), type.getName(), type.isSymmetric(), type.isCustom());
    }
}
