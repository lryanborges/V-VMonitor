package com.vvmonitor.service;

import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.VersionNotFoundException;
import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.RelationshipSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TestSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TypeSnapshot;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.ProjectVersionRepository;
import com.vvmonitor.infra.repository.RestoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Restauracao de uma versao (RF22, proposto para a v2 do documento): o modelo atual volta a ser igual ao da
 * versao. Nao cria nem altera versoes (RNF2): o resultado aparece como mudancas pendentes em relacao a ultima
 * versao, e as alteracoes que ainda nao tinham virado versao se perdem (o frontend avisa antes).
 */
@Service
public class RestoreService {

    private final RestoreRepository restoreRepository;
    private final ProjectVersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;

    public RestoreService(RestoreRepository restoreRepository, ProjectVersionRepository versionRepository,
                          ProjectRepository projectRepository, ProjectAccessService accessService) {
        this.restoreRepository = restoreRepository;
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
    }

    /**
     * Numa transacao: exclui (logicamente) o que nao existia na versao, reativa com o mesmo id e codigo o que tinha
     * sido excluido e volta ao conteudo da versao o que foi alterado, inclusive a cobertura dos testes.
     */
    @Transactional
    public void restore(UUID projectId, int number, UUID userId) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        // serializa com submissoes e outras restauracoes do mesmo projeto
        versionRepository.lockProject(projectId);
        ModelSnapshot version = versionRepository.findSnapshot(projectId, number)
                .orElseThrow(() -> new VersionNotFoundException(number));

        // 1. sai o que foi criado depois da versao (elementos levam junto relacoes e vinculos)
        removeExtra("elements", projectId, ids(version.elements(), ElementSnapshot::id));
        removeExtra("tests", projectId, ids(version.tests(), TestSnapshot::id));
        removeExtra("relationships", projectId, ids(version.relationships(), RelationshipSnapshot::id));

        // 2. volta o que existia, como estava (reativando o que foi excluido)
        restoreRepository.restoreTypes(projectId, version.types().stream()
                .filter(TypeSnapshot::custom).map(TypeSnapshot::id).toList());
        version.elements().forEach(e -> restoreRepository.restoreElement(projectId, e.id(), e.description(), e.priority()));
        version.relationships().forEach(r ->
                restoreRepository.restoreRelationship(projectId, r.id(), r.sourceId(), r.targetId(), r.typeId()));
        version.tests().forEach(t -> {
            restoreRepository.restoreTest(projectId, t.id(), t.description());
            restoreRepository.restoreCoverage(t.id(), t.elementIds());
        });
        projectRepository.touch(projectId);
    }

    private void removeExtra(String table, UUID projectId, Set<UUID> keep) {
        List<UUID> extra = restoreRepository.activeIds(table, projectId).stream().filter(id -> !keep.contains(id)).toList();
        restoreRepository.softDelete(table, projectId, extra);
    }

    private static <T> Set<UUID> ids(List<T> items, Function<T, UUID> id) {
        return items.stream().map(id).collect(Collectors.toSet());
    }
}
