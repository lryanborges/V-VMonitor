package com.vvmonitor.domain.exception;

import java.util.UUID;

public class ElementNotFoundException extends ResourceNotFoundException {

    public ElementNotFoundException(UUID id) {
        super("Elemento " + id + " não encontrado.");
    }
}
