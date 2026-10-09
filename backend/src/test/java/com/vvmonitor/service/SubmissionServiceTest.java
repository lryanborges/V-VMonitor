package com.vvmonitor.service;

import com.vvmonitor.api.dto.response.SubmissionResponse;
import com.vvmonitor.domain.entity.Project;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.NothingToSubmitException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T20:00:00Z");

    private final UUID userId = UUID.randomUUID();
    private Project project;
    private ElementRepository elementRepository;
    private RelationshipRepository relationshipRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private SubmissionService service;

    @BeforeEach
    void setUp() {
        elementRepository = mock(ElementRepository.class);
        relationshipRepository = mock(RelationshipRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        service = new SubmissionService(elementRepository, relationshipRepository, projectRepository, userRepository,
                new ProjectAccessService(memberRepository), Clock.fixed(NOW, ZoneOffset.UTC));

        project = new Project("Agenda", null, userId);
        ReflectionTestUtils.setField(project, "id", UUID.randomUUID());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User("Ana Lima", "ana@exemplo.com", "hash")));
        loggedAs(MemberRole.EDITOR);
    }

    private void loggedAs(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(project.getId(), userId))
                .thenReturn(Optional.of(new ProjectMember(project.getId(), userId, role)));
    }

    @Test
    void submeteRascunhosERegistraQuemEQuando() {
        when(elementRepository.submitDrafts(project.getId())).thenReturn(7);
        when(relationshipRepository.submitDrafts(project.getId())).thenReturn(4);

        SubmissionResponse response = service.submit(project.getId(), userId);

        assertThat(response.elements()).isEqualTo(7);
        assertThat(response.relationships()).isEqualTo(4);
        assertThat(response.submittedAt()).isEqualTo(NOW);
        assertThat(response.submittedBy().name()).isEqualTo("Ana Lima");
        assertThat(project.getLastSubmittedAt()).isEqualTo(NOW);
        assertThat(project.getLastSubmittedBy()).isEqualTo(userId);
        verify(projectRepository).save(project);
    }

    @Test
    void soRelacoesPendentesTambemSubmete() {
        when(elementRepository.submitDrafts(project.getId())).thenReturn(0);
        when(relationshipRepository.submitDrafts(project.getId())).thenReturn(2);

        assertThat(service.submit(project.getId(), userId).relationships()).isEqualTo(2);
    }

    @Test
    void semPendenciasRespondeNadaASubmeterSemRegistrar() {
        assertThatThrownBy(() -> service.submit(project.getId(), userId))
                .isInstanceOf(NothingToSubmitException.class)
                .hasMessage("Não há alterações para submeter.");
        assertThat(project.getLastSubmittedAt()).isNull();
        verify(projectRepository, never()).save(any());
    }

    @Test
    void visualizadorNaoSubmete() {
        loggedAs(MemberRole.VIEWER);

        assertThatThrownBy(() -> service.submit(project.getId(), userId))
                .isInstanceOf(ProjectAccessDeniedException.class);
        verify(elementRepository, never()).submitDrafts(any());
    }
}
