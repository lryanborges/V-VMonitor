package com.vvmonitor.domain.exception;

/** RF10: o modelo nao tem elementos nem relacoes em rascunho. */
public class NothingToSubmitException extends ConflictException {

    public NothingToSubmitException() {
        super("Não há alterações para submeter.");
    }
}
