package com.vvmonitor.service;

import com.vvmonitor.api.dto.response.ProjectResponse;
import com.vvmonitor.api.dto.response.SubmissionResponse;
import com.vvmonitor.domain.entity.Project;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.NothingToSubmitException;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** RF10: submissao do modelo. Rascunhos passam a submetidos e entram na visualizacao (UC-07). */
@Service
public class SubmissionService {

    private final ElementRepository elementRepository;
    private final RelationshipRepository relationshipRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final Clock clock;

    @Autowired
    public SubmissionService(ElementRepository elementRepository, RelationshipRepository relationshipRepository,
                             ProjectRepository projectRepository, UserRepository userRepository,
                             ProjectAccessService accessService) {
        this(elementRepository, relationshipRepository, projectRepository, userRepository, accessService,
                Clock.systemUTC());
    }

    /** Relogio injetavel para testes. */
    SubmissionService(ElementRepository elementRepository, RelationshipRepository relationshipRepository,
                      ProjectRepository projectRepository, UserRepository userRepository,
                      ProjectAccessService accessService, Clock clock) {
        this.elementRepository = elementRepository;
        this.relationshipRepository = relationshipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.clock = clock;
    }

    /**
     * Submete de uma vez todos os elementos e relacoes em rascunho, numa unica transacao, e registra
     * quem submeteu e quando. Sem nada pendente, responde 409.
     */
    @Transactional
    public SubmissionResponse submit(UUID projectId, UUID userId) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        int elements = elementRepository.submitDrafts(projectId);
        int relationships = relationshipRepository.submitDrafts(projectId);
        if (elements == 0 && relationships == 0) {
            throw new NothingToSubmitException();
        }

        Instant now = clock.instant();
        project.registerSubmission(userId, now);
        projectRepository.save(project);

        String name = userRepository.findById(userId).map(u -> u.getName()).orElse(null);
        return new SubmissionResponse(elements, relationships, now, new ProjectResponse.PersonSummary(userId, name));
    }
}
