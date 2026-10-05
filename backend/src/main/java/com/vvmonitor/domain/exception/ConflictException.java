package com.vvmonitor.domain.exception;

/** Registro duplicado ou conflitante com o estado atual (ex.: e-mail ja cadastrado); vira 409. */
public abstract class ConflictException extends BusinessException {

    protected ConflictException(String message) {
        super(message);
    }
}
