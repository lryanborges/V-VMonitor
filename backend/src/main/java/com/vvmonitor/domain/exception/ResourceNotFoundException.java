package com.vvmonitor.domain.exception;

/** Recurso inexistente, removido ou fora do alcance do usuario; vira 404. */
public abstract class ResourceNotFoundException extends BusinessException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
