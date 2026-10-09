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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectStatsRepository statsRepository;
    private final ProjectAccessService accessService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                          ProjectStatsRepository statsRepository, ProjectAccessService accessService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.statsRepository = statsRepository;
        this.accessService = accessService;
    }

    /** RF3, UC-04 passo 2: projetos aos quais o usuario tem acesso, proprios e compartilhados. */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listAccessible(UUID userId) {
        return toResponses(projectRepository.findAccessibleBy(userId), userId);
    }

    /** RF4, UC-03 passos 4 e 5: cria o projeto e registra o criador como OWNER. */
    @Transactional
    public ProjectResponse create(UUID userId, CreateProjectRequest request) {
        Project project = projectRepository.save(new Project(request.name(), request.description(), userId));
        memberRepository.save(new ProjectMember(project.getId(), userId, MemberRole.OWNER));
        // flush para que created_at/updated_at e o membro existam antes de montar a resposta
        projectRepository.flush();
        return toResponses(List.of(project), userId).getFirst();
    }

    /** UC-04 passo 4: dados do projeto selecionado; 404 se o usuario nao for membro. */
    @Transactional(readOnly = true)
    public ProjectResponse findById(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        return toResponses(List.of(project), userId).getFirst();
    }

    /** Monta as respostas com membros e numeros de todos os projetos em duas consultas, sem N+1. */
    private List<ProjectResponse> toResponses(List<Project> projects, UUID userId) {
        if (projects.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = projects.stream().map(Project::getId).toList();
        Map<UUID, List<MemberView>> membersByProject = memberRepository.findMembersOf(ids).stream()
                .collect(Collectors.groupingBy(MemberView::projectId));
        Map<UUID, ProjectStatsView> statsByProject = statsRepository.findByProjectIds(ids);

        return projects.stream().map(project -> {
            List<MemberView> members = membersByProject.getOrDefault(project.getId(), List.of());
            ProjectStatsView stats = statsByProject.getOrDefault(project.getId(), ProjectStatsView.empty(project.getId()));
            return toResponse(project, members, stats, userId);
        }).toList();
    }

    private static ProjectResponse toResponse(Project project, List<MemberView> members, ProjectStatsView stats,
                                              UUID userId) {
        MemberRole myRole = members.stream()
                .filter(m -> m.userId().equals(userId))
                .map(MemberView::role)
                .findFirst()
                .orElseThrow(() -> new ProjectNotFoundException(project.getId()));
        String ownerName = nameOf(project.getOwnerId(), members);
        ProjectResponse.PersonSummary submittedBy = project.getLastSubmittedBy() == null
                ? null
                : new ProjectResponse.PersonSummary(project.getLastSubmittedBy(),
                        nameOf(project.getLastSubmittedBy(), members));

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                myRole,
                new ProjectResponse.PersonSummary(project.getOwnerId(), ownerName),
                members.stream()
                        .map(m -> new ProjectResponse.MemberSummary(m.userId(), m.name(), m.role()))
                        .toList(),
                ProjectResponse.Stats.of(stats.requirements(), stats.businessRules(), stats.tests(),
                        stats.requirementsWithTests()),
                stats.latestVersion(),
                project.getLastSubmittedAt(),
                submittedBy,
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    /** Nome de um membro do projeto (ex.: dono, quem submeteu); null se a pessoa nao for mais membro. */
    private static String nameOf(UUID userId, List<MemberView> members) {
        return members.stream().filter(m -> m.userId().equals(userId)).map(MemberView::name).findFirst().orElse(null);
    }
}
