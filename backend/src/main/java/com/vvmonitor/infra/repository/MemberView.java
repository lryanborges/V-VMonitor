package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.enums.MemberRole;

import java.util.UUID;

/** Membro de um projeto ja com o nome do usuario, para montar as respostas sem uma consulta por membro. */
public record MemberView(UUID projectId, UUID userId, String name, MemberRole role) {
}
