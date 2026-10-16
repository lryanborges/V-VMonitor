package com.vvmonitor.domain.exception;

import java.util.UUID;

/** RF20: a pessoa nao e (ou deixou de ser) membro do projeto. */
public class MemberNotFoundException extends ResourceNotFoundException {

    public MemberNotFoundException(UUID userId) {
        super("Membro " + userId + " não encontrado neste projeto.");
    }
}
