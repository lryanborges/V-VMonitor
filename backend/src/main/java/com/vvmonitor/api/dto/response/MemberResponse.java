package com.vvmonitor.api.dto.response;

import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.infra.repository.MemberDetailView;

import java.time.Instant;
import java.util.UUID;

/** RF20: pessoa com acesso ao projeto, na janela de compartilhamento. */
public record MemberResponse(UUID userId, String name, String email, MemberRole role, Instant addedAt) {

    public static MemberResponse from(MemberDetailView view) {
        return new MemberResponse(view.userId(), view.name(), view.email(), view.role(), view.addedAt());
    }
}
