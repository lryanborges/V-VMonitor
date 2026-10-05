package com.vvmonitor.domain.exception;

/** UC-06, excecao do passo 6: os elementos ja possuem esse relacionamento; registro duplicado nao e feito. */
public class DuplicateRelationshipException extends ConflictException {

    public DuplicateRelationshipException(String sentence) {
        super(sentence + " Registro duplicado não é permitido.");
    }
}
