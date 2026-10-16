package com.vvmonitor.domain.exception;

/** RF20: a pessoa ja tem acesso ao projeto. */
public class AlreadyMemberException extends ConflictException {

    public AlreadyMemberException(String email) {
        super(email + " já tem acesso a este projeto.");
    }
}
