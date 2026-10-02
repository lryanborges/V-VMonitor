package com.vvmonitor.domain.exception;

/** Violacao de regra de negocio, com mensagem apresentavel ao usuario. */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
