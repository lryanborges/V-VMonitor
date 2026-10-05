package com.vvmonitor.domain.exception;

/**
 * Regra de negocio violada por um campo especifico, que depende de outros campos para ser validada
 * (ex.: prioridade obrigatoria so para requisitos). Vira 400 com o erro associado ao campo.
 */
public class InvalidFieldException extends BusinessException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
