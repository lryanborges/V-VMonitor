package com.vvmonitor.domain.exception;

public class RelationshipTypeAlreadyExistsException extends ConflictException {

    public RelationshipTypeAlreadyExistsException(String name) {
        super("Já existe um tipo de relacionamento chamado “" + name + "”.");
    }
}
