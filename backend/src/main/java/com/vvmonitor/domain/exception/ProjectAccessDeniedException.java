package com.vvmonitor.domain.exception;

import com.vvmonitor.domain.enums.MemberRole;

/** Membro do projeto sem a permissao exigida pela acao (ex.: VIEWER tentando editar); vira 403. */
public class ProjectAccessDeniedException extends BusinessException {

    public ProjectAccessDeniedException(MemberRole required) {
        super("Você não tem permissão para esta ação. Permissão necessária: " + required + ".");
    }
}
