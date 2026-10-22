package com.vvmonitor.domain.exception;

import java.util.UUID;

public class TestNotFoundException extends ResourceNotFoundException {

    public TestNotFoundException(UUID id) {
        super("Teste " + id + " não encontrado.");
    }
}
