package com.vvmonitor.domain.exception;

/** RF20: o proprietario nao sai nem e removido do proprio projeto. */
public class OwnerCannotBeRemovedException extends ConflictException {

    public OwnerCannotBeRemovedException() {
        super("O proprietário não pode ser removido do projeto.");
    }
}
