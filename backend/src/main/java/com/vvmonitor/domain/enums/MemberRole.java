package com.vvmonitor.domain.enums;

/** Permissao de um membro no projeto, da maior para a menor (RF20, RNF4). */
public enum MemberRole {
    OWNER,
    EDITOR,
    VIEWER;

    /** true se esta permissao inclui a permissao pedida (ex.: OWNER inclui EDITOR). */
    public boolean includes(MemberRole required) {
        return this.ordinal() <= required.ordinal();
    }
}
