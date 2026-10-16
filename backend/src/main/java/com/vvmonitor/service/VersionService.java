package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateVersionRequest;
import com.vvmonitor.api.dto.response.ModelChangesResponse;
import com.vvmonitor.api.dto.response.VersionResponse;
import com.vvmonitor.api.dto.response.VersionSummaryResponse;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.NothingToSubmitException;
import com.vvmonitor.domain.exception.VersionNotFoundException;
import com.vvmonitor.domain.version.ModelChanges;
import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.ProjectVersionRepository;
import com.vvmonitor.infra.repository.ProjectVersionRepository.Latest;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.RelationshipTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Submissao do modelo como versao imutavel (RF10 + RF17, RNF2) e consulta das versoes (RF18).
 * O que esta pendente (rascunho) e o que difere da ultima versao: adicionado, alterado ou removido.
 */
@Service
public class VersionService {

    private final ElementRepository elementRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipTypeRepository typeRepository;
    private final ProjectVersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;
    private final Clock clock;

    @Autowired
    public VersionService(ElementRepository elementRepository, RelationshipRepository relationshipRepository,
                          RelationshipTypeRepository typeRepository, ProjectVersionRepository versionRepository,
                          ProjectRepository projectRepository, ProjectAccessService accessService) {
        this(elementRepository, relationshipRepository, typeRepository, versionRepository, projectRepository,
                accessService, Clock.systemUTC());
    }

    /** Relogio injetavel para testes. */
    VersionService(ElementRepository elementRepository, RelationshipRepository relationshipRepository,
                   RelationshipTypeRepository typeRepository, ProjectVersionRepository versionRepository,
                   ProjectRepository projectRepository, ProjectAccessService accessService, Clock clock) {
        this.elementRepository = elementRepository;
        this.relationshipRepository = relationshipRepository;
        this.typeRepository = typeRepository;
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.clock = clock;
    }

    /** O que mudou desde a ultima versao, ou seja, o que entra na proxima submissao. */
    @Transactional(readOnly = true)
    public ModelChangesResponse changes(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        State state = state(projectId);
        return ModelChangesResponse.from(state.baseNumber(), null, state.base(), state.current(), state.changes());
    }

    /**
     * RF10 + RF17: grava o modelo atual como a proxima versao (v1, v2...), com nome opcional e o resumo das
     * mudancas. Sem mudancas desde a ultima versao, responde 409.
     */
    @Transactional
    public VersionSummaryResponse create(UUID projectId, UUID userId, CreateVersionRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        // serializa submissoes do mesmo projeto antes de ler a ultima versao
        versionRepository.lockProject(projectId);
        State state = state(projectId);
        if (state.changes().isEmpty()) {
            throw new NothingToSubmitException();
        }
        int number = state.baseNumber() == null ? 1 : state.baseNumber() + 1;
        versionRepository.insert(projectId, number, normalizeLabel(request.label()),
                state.current().withChanges(state.changes().summary()), userId, clock.instant());
        projectRepository.touch(projectId);
        return VersionSummaryResponse.from(versionRepository.find(projectId, number).orElseThrow());
    }

    /** RF18: versoes do projeto, mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<VersionSummaryResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        return versionRepository.findAll(projectId).stream().map(VersionSummaryResponse::from).toList();
    }

    /** RF18: uma versao com o modelo completo daquele momento. */
    @Transactional(readOnly = true)
    public VersionResponse find(UUID projectId, int number, UUID userId) {
        accessService.requireMember(projectId, userId);
        VersionSummaryResponse summary = versionRepository.find(projectId, number)
                .map(VersionSummaryResponse::from)
                .orElseThrow(() -> new VersionNotFoundException(number));
        ModelSnapshot snapshot = versionRepository.findSnapshot(projectId, number)
                .orElseThrow(() -> new VersionNotFoundException(number));
        return VersionResponse.from(summary, snapshot);
    }

    /**
     * RF18: compara a versao "number" com outra. with = "previous" (padrao; o que essa versao trouxe em relacao
     * a anterior), "current" (o que mudou dela ate o modelo atual) ou o numero de outra versao (sempre da
     * mais antiga para a mais nova).
     */
    @Transactional(readOnly = true)
    public ModelChangesResponse compare(UUID projectId, int number, String with, UUID userId) {
        accessService.requireMember(projectId, userId);
        ModelSnapshot version = snapshot(projectId, number);
        if (with == null || with.isBlank() || with.equals("previous")) {
            Integer previous = number > 1 ? number - 1 : null;
            ModelSnapshot base = previous == null ? ModelSnapshot.EMPTY : snapshot(projectId, previous);
            return ModelChangesResponse.from(previous, number, base, version, ModelChanges.between(base, version));
        }
        if (with.equals("current")) {
            ModelSnapshot current = currentSnapshot(projectId);
            return ModelChangesResponse.from(number, null, version, current, ModelChanges.between(version, current));
        }
        int other;
        try {
            other = Integer.parseInt(with);
        } catch (NumberFormatException e) {
            throw new InvalidFieldException("with", "Use \"previous\", \"current\" ou o número de uma versão.");
        }
        ModelSnapshot otherVersion = snapshot(projectId, other);
        // da mais antiga para a mais nova
        boolean older = number <= other;
        ModelSnapshot base = older ? version : otherVersion;
        ModelSnapshot target = older ? otherVersion : version;
        return ModelChangesResponse.from(Math.min(number, other), Math.max(number, other), base, target,
                ModelChanges.between(base, target));
    }

    private ModelSnapshot snapshot(UUID projectId, int number) {
        return versionRepository.findSnapshot(projectId, number).orElseThrow(() -> new VersionNotFoundException(number));
    }

    /**
     * Ids dos elementos e relacoes que diferem da ultima versao (adicionados ou alterados), exibidos como
     * rascunho nas listas, no grafo e na matriz. Nao verifica acesso: quem chama ja verificou.
     */
    @Transactional(readOnly = true)
    public Set<UUID> pendingIds(UUID projectId) {
        return state(projectId).changes().pendingIds();
    }

    private ModelSnapshot currentSnapshot(UUID projectId) {
        Map<UUID, RelationshipType> types = typeRepository.findAvailableFor(projectId).stream()
                .collect(Collectors.toMap(RelationshipType::getId, Function.identity()));
        return ModelSnapshot.of(elementRepository.findAllByProjectId(projectId),
                relationshipRepository.findAllByProjectIdOrderByCreatedAt(projectId), types);
    }

    private State state(UUID projectId) {
        ModelSnapshot current = currentSnapshot(projectId);
        Optional<Latest> latest = versionRepository.findLatest(projectId);
        ModelSnapshot base = latest.map(Latest::snapshot).orElse(ModelSnapshot.EMPTY);
        return new State(latest.map(Latest::number).orElse(null), base, current, ModelChanges.between(base, current));
    }

    /** baseNumber: numero da ultima versao (null antes da primeira); base: o conteudo dela. */
    private record State(Integer baseNumber, ModelSnapshot base, ModelSnapshot current, ModelChanges changes) {
    }

    private static String normalizeLabel(String label) {
        return label == null || label.isBlank() ? null : label.strip();
    }
}
