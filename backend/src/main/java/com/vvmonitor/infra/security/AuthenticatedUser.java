package com.vvmonitor.infra.security;

import java.util.UUID;

/** Usuario autenticado da requisicao atual, extraido do token. Obtido nos controllers via @AuthenticationPrincipal. */
public record AuthenticatedUser(UUID id, String email) {
}
