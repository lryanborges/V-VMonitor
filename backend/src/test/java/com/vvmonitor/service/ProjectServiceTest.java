package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateProjectRequest;
import com.vvmonitor.api.dto.response.ProjectResponse;
import com.vvmonitor.domain.entity.Project;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.MemberView;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.ProjectStatsRepository;
import com.vvmonitor.infra.repository.ProjectStatsRepository.ProjectStatsView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private final UUID ana = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();

    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private ProjectStatsRepository statsRepository;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        statsRepository = mock(ProjectStatsRepository.class);
        projectService = new ProjectService(projectRepository, memberRepository, statsRepository,
                new ProjectAccessService(memberRepository));
        when(memberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());
    }

    private static Project projectOwnedBy(UUID ownerId, String name) {
        Project project = new Project(name, "  descrição  ", ownerId);
        ReflectionTestUtils.setField(project, "id", UUID.randomUUID());
        return project;
    }

    @Test
    void criarProjetoRegistraOCriadorComoOwner() {
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
        when(memberRepository.findMembersOf(any())).thenAnswer(inv -> {
            UUID projectId = ((List<UUID>) inv.getArgument(0)).getFirst();
            return List.of(new MemberView(projectId, ana, "Ana", MemberRole.OWNER));
        });
        when(statsRepository.findByProjectIds(any())).thenReturn(Map.of());

        ProjectResponse response = projectService.create(ana, new CreateProjectRequest("  Agenda clínica ", "   "));

        ArgumentCaptor<ProjectMember> member = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberRepository).save(member.capture());
        assertThat(member.getValue().getUserId()).isEqualTo(ana);
        assertThat(member.getValue().getRole()).isEqualTo(MemberRole.OWNER);

        assertThat(response.name()).isEqualTo("Agenda clínica");
        assertThat(response.description()).isNull();
        assertThat(response.role()).isEqualTo(MemberRole.OWNER);
        assertThat(response.owner().name()).isEqualTo("Ana");
        assertThat(response.stats().coverage()).isZero();
        assertThat(response.latestVersion()).isNull();
    }

    @Test
    void listagemMostraProjetoCompartilhadoComDonoENumeros() {
        Project shared = projectOwnedBy(ana, "Portal do aluno");
        when(projectRepository.findAccessibleBy(bruno)).thenReturn(List.of(shared));
        when(memberRepository.findMembersOf(any())).thenReturn(List.of(
                new MemberView(shared.getId(), ana, "Ana Lima", MemberRole.OWNER),
                new MemberView(shared.getId(), bruno, "Bruno", MemberRole.EDITOR)));
        when(statsRepository.findByProjectIds(any())).thenReturn(Map.of(
                shared.getId(), new ProjectStatsView(shared.getId(), 48, 12, 31, 28, 3)));

        ProjectResponse response = projectService.listAccessible(bruno).getFirst();

        assertThat(response.role()).isEqualTo(MemberRole.EDITOR);
        assertThat(response.owner().name()).isEqualTo("Ana Lima");
        assertThat(response.description()).isEqualTo("descrição");
        assertThat(response.members()).extracting(ProjectResponse.MemberSummary::name)
                .containsExactly("Ana Lima", "Bruno");
        assertThat(response.stats().requirements()).isEqualTo(48);
        assertThat(response.stats().businessRules()).isEqualTo(12);
        assertThat(response.stats().tests()).isEqualTo(31);
        assertThat(response.stats().coverage()).isEqualTo(58); // 28 de 48
        assertThat(response.latestVersion()).isEqualTo(3);
    }

    @Test
    void semProjetosDevolveListaVaziaSemConsultarMembros() {
        when(projectRepository.findAccessibleBy(ana)).thenReturn(List.of());

        assertThat(projectService.listAccessible(ana)).isEmpty();
        verify(memberRepository, never()).findMembersOf(any());
    }

    @Test
    void naoMembroNaoVeOProjeto() {
        UUID projectId = UUID.randomUUID();

        assertThatThrownBy(() -> projectService.findById(projectId, bruno))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void coberturaArredondadaEZeroSemRequisitos() {
        assertThat(ProjectResponse.Stats.of(0, 4, 2, 0).coverage()).isZero();
        assertThat(ProjectResponse.Stats.of(30, 4, 10, 9).coverage()).isEqualTo(30);
        assertThat(ProjectResponse.Stats.of(3, 0, 1, 1).coverage()).isEqualTo(33);
    }
}
