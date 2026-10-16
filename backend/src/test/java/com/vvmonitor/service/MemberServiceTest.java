package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.AddMemberRequest;
import com.vvmonitor.api.dto.response.MemberResponse;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.AlreadyMemberException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.MemberNotFoundException;
import com.vvmonitor.domain.exception.OwnerCannotBeRemovedException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();
    private final UUID editor = UUID.randomUUID();
    private final UUID otherEditor = UUID.randomUUID();
    private ProjectMemberRepository memberRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private MemberService service;
    private ProjectMember editorMember;

    @BeforeEach
    void setUp() {
        memberRepository = mock(ProjectMemberRepository.class);
        userRepository = mock(UserRepository.class);
        projectRepository = mock(ProjectRepository.class);
        service = new MemberService(memberRepository, userRepository, projectRepository,
                new ProjectAccessService(memberRepository));

        when(memberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());
        member(owner, MemberRole.OWNER);
        editorMember = member(editor, MemberRole.EDITOR);
        member(otherEditor, MemberRole.EDITOR);
        when(memberRepository.saveAndFlush(any(ProjectMember.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProjectMember member(UUID userId, MemberRole role) {
        ProjectMember m = new ProjectMember(projectId, userId, role);
        when(memberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(m));
        return m;
    }

    private User user(String name, String email) {
        User u = new User(name, email, "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void proprietarioAdicionaPessoaCadastradaComoEditor() {
        User carla = user("Carla", "carla@exemplo.com");

        MemberResponse response = service.add(projectId, owner, new AddMemberRequest("  Carla@Exemplo.com "));

        ArgumentCaptor<ProjectMember> saved = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(carla.getId());
        assertThat(saved.getValue().getRole()).isEqualTo(MemberRole.EDITOR);
        assertThat(response.email()).isEqualTo("carla@exemplo.com");
        verify(projectRepository).touch(projectId);
    }

    @Test
    void emailSemCadastroERecusadoNoCampoEmail() {
        assertThatThrownBy(() -> service.add(projectId, owner, new AddMemberRequest("ninguem@exemplo.com")))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("não tem cadastro");
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void quemJaTemAcessoNaoEAdicionadoDeNovo() {
        User ja = user("Edu", "edu@exemplo.com");
        ReflectionTestUtils.setField(ja, "id", editor);

        assertThatThrownBy(() -> service.add(projectId, owner, new AddMemberRequest("edu@exemplo.com")))
                .isInstanceOf(AlreadyMemberException.class);
    }

    @Test
    void editorNaoAdiciona() {
        user("Carla", "carla@exemplo.com");

        assertThatThrownBy(() -> service.add(projectId, editor, new AddMemberRequest("carla@exemplo.com")))
                .isInstanceOf(ProjectAccessDeniedException.class);
    }

    @Test
    void proprietarioRemoveEditor() {
        service.remove(projectId, owner, editor);

        verify(memberRepository).delete(editorMember);
    }

    @Test
    void editorSaiDoProjeto() {
        service.remove(projectId, editor, editor);

        verify(memberRepository).delete(editorMember);
    }

    @Test
    void editorNaoRemoveOutroEditor() {
        assertThatThrownBy(() -> service.remove(projectId, editor, otherEditor))
                .isInstanceOf(ProjectAccessDeniedException.class);
        verify(memberRepository, never()).delete(any());
    }

    @Test
    void proprietarioNaoPodeSerRemovido() {
        assertThatThrownBy(() -> service.remove(projectId, owner, owner))
                .isInstanceOf(OwnerCannotBeRemovedException.class);
        assertThatThrownBy(() -> service.remove(projectId, editor, owner))
                .isInstanceOf(OwnerCannotBeRemovedException.class);
        verify(memberRepository, never()).delete(any());
    }

    @Test
    void removerQuemNaoEMembroRetorna404ENaoMembroNaoVeOProjeto() {
        assertThatThrownBy(() -> service.remove(projectId, owner, UUID.randomUUID()))
                .isInstanceOf(MemberNotFoundException.class);
        assertThatThrownBy(() -> service.list(projectId, UUID.randomUUID()))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
