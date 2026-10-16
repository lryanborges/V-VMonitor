package com.vvmonitor.infra.repository;

import com.vvmonitor.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/** Membro de um projeto com nome e e-mail, para a lista de pessoas com acesso (RF20). */
public record MemberDetailView(UUID userId, String name, String email, MemberRole role, Instant addedAt) {
}
