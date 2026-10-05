package com.vvmonitor.domain.exception;

import java.util.UUID;

/** Tipo inexistente ou personalizado de outro projeto. */
public class RelationshipTypeNotFoundException extends ResourceNotFoundException {

    public RelationshipTypeNotFoundException(UUID id) {
        super("Tipo de relacionamento " + id + " não encontrado.");
    }
}
