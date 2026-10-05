package com.vvmonitor.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(UUID id) {
        super("Usuário " + id + " não encontrado.");
    }
}
