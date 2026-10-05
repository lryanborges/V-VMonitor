package com.vvmonitor.domain.exception;

import java.util.UUID;

public class RelationshipNotFoundException extends ResourceNotFoundException {

    public RelationshipNotFoundException(UUID id) {
        super("Relacionamento " + id + " não encontrado.");
    }
}
