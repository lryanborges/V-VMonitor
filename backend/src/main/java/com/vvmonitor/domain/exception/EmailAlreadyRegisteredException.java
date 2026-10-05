package com.vvmonitor.domain.exception;

/** UC-01, excecao do passo 4: e-mail ja cadastrado. */
public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("O e-mail " + email + " já está cadastrado.");
    }
}
