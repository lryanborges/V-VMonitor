package com.vvmonitor.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(UUID id) {
        super("Usuário " + id + " não encontrado.");
    }
}
