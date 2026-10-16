package com.vvmonitor.domain.exception;

/** RF18: versao inexistente no projeto. */
public class VersionNotFoundException extends ResourceNotFoundException {

    public VersionNotFoundException(int number) {
        super("Versão v" + number + " não encontrada.");
    }
}
