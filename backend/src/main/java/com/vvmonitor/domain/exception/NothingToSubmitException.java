package com.vvmonitor.domain.exception;

/** RF10/RF17: o modelo nao mudou desde a ultima versao (ou esta vazio antes da primeira). */
public class NothingToSubmitException extends ConflictException {

    public NothingToSubmitException() {
        super("Não há alterações desde a última versão.");
    }
}
