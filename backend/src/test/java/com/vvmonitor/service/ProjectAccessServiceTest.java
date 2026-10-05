package com.vvmonitor.service;

import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectAccessServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private ProjectMemberRepository memberRepository;
    private ProjectAccessService accessService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(ProjectMemberRepository.class);
        accessService = new ProjectAccessService(memberRepository);
        when(memberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());
    }

    private void memberWith(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, role)));
    }

    @Test
    void naoMembroRecebeProjetoNaoEncontrado() {
        assertThatThrownBy(() -> accessService.requireMember(projectId, userId))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void membroTemAcesso() {
        memberWith(MemberRole.VIEWER);

        assertThat(accessService.requireMember(projectId, userId).getRole()).isEqualTo(MemberRole.VIEWER);
    }

    @Test
    void hierarquiaDePermissoes() {
        assertThat(MemberRole.OWNER.includes(MemberRole.EDITOR)).isTrue();
        assertThat(MemberRole.OWNER.includes(MemberRole.OWNER)).isTrue();
        assertThat(MemberRole.EDITOR.includes(MemberRole.VIEWER)).isTrue();
        assertThat(MemberRole.EDITOR.includes(MemberRole.OWNER)).isFalse();
        assertThat(MemberRole.VIEWER.includes(MemberRole.EDITOR)).isFalse();
    }

    @Test
    void visualizadorNaoPodeEditar() {
        memberWith(MemberRole.VIEWER);

        assertThatThrownBy(() -> accessService.requireRole(projectId, userId, MemberRole.EDITOR))
                .isInstanceOf(ProjectAccessDeniedException.class);
    }

    @Test
    void donoPodeEditar() {
        memberWith(MemberRole.OWNER);

        assertThat(accessService.requireRole(projectId, userId, MemberRole.EDITOR).getRole())
                .isEqualTo(MemberRole.OWNER);
    }
}
