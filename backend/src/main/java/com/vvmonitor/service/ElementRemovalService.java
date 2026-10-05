package com.vvmonitor.service;

import com.vvmonitor.api.dto.response.DeleteImpactResponse;
import com.vvmonitor.api.dto.response.DeleteImpactResponse.ImpactLevel;
import com.vvmonitor.api.dto.response.DeleteImpactResponse.ImpactRelationship;
import com.vvmonitor.api.dto.response.DeleteImpactResponse.TestSummary;
import com.vvmonitor.api.dto.response.ElementSummary;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.ElementNotFoundException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository.ElementStats;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.RelationshipTypeRepository;
import com.vvmonitor.infra.repository.TestCoverageQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Exclusao de elementos (RF15) e o alerta de impacto que a antecede (RF16). */
@Service
public class ElementRemovalService {

    /** A partir de quantas relacoes a exclusao e considerada de impacto alto. */
    static final int HIGH_IMPACT_RELATIONSHIPS = 5;

    private final ElementRepository elementRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipTypeRepository typeRepository;
    private final TestCoverageQueryRepository coverageRepository;
    private final ElementStatsRepository statsRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;

    public ElementRemovalService(ElementRepository elementRepository, RelationshipRepository relationshipRepository,
                                 RelationshipTypeRepository typeRepository,
                                 TestCoverageQueryRepository coverageRepository,
                                 ElementStatsRepository statsRepository, ProjectRepository projectRepository,
                                 ProjectAccessService accessService) {
        this.elementRepository = elementRepository;
        this.relationshipRepository = relationshipRepository;
        this.typeRepository = typeRepository;
        this.coverageRepository = coverageRepository;
        this.statsRepository = statsRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
    }

    /** RF16: calcula o que muda no modelo se o elemento for excluido, sem alterar nada. */
    @Transactional(readOnly = true)
    public DeleteImpactResponse impact(UUID projectId, UUID elementId, UUID userId) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Element element = find(projectId, elementId);

        Map<UUID, Element> elements = elementRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(Element::getId, Function.identity()));
        Map<UUID, RelationshipType> types = typeRepository.findAvailableFor(projectId).stream()
                .collect(Collectors.toMap(RelationshipType::getId, Function.identity()));
        // relacoes ativas entre elementos ativos
        List<Relationship> all = relationshipRepository.findAllByProjectIdOrderByCreatedAt(projectId).stream()
                .filter(r -> elements.containsKey(r.getSourceId()) && elements.containsKey(r.getTargetId()))
                .toList();
        List<Relationship> own = all.stream().filter(r -> r.touches(elementId)).toList();

        List<ImpactRelationship> relationships = own.stream()
                .map(r -> new ImpactRelationship(r.getId(), types.get(r.getTypeId()).getName(),
                        r.getSourceId().equals(elementId), ElementSummary.from(elements.get(r.otherEnd(elementId)))))
                .toList();

        // vizinhos que so se ligam ao elemento excluido ficam sem vinculo algum
        Set<UUID> neighbours = own.stream().map(r -> r.otherEnd(elementId)).collect(Collectors.toCollection(LinkedHashSet::new));
        List<ElementSummary> orphans = neighbours.stream()
                .filter(n -> all.stream().noneMatch(r -> r.touches(n) && !r.touches(elementId)))
                .map(n -> ElementSummary.from(elements.get(n)))
                .sorted(Comparator.comparing(ElementSummary::code))
                .toList();

        // quem depende do excluido (relacao "X depende de excluido") perde uma dependencia
        List<ElementSummary> dependents = own.stream()
                .filter(r -> r.getTargetId().equals(elementId) && types.get(r.getTypeId()).isDependency())
                .map(r -> ElementSummary.from(elements.get(r.getSourceId())))
                .distinct()
                .toList();

        List<TestSummary> orphanTests = coverageRepository.testsOnlyCovering(elementId).stream()
                .map(t -> new TestSummary(t.id(), t.code(), t.description()))
                .toList();
        long tests = statsRepository.findByProjectId(projectId).getOrDefault(element.getId(), ElementStats.NONE).tests();

        ImpactLevel level = impactLevel(relationships.size(), tests, orphans.size(), orphanTests.size());
        return new DeleteImpactResponse(level, relationships, orphans, dependents, orphanTests, tests);
    }

    /**
     * Regra inicial (a refinar): LOW sem relacoes nem testes; HIGH se algum elemento fica orfao, algum teste
     * fica sem requisito ou ha muitas relacoes; MEDIUM nos demais casos.
     */
    static ImpactLevel impactLevel(int relationships, long tests, int orphans, int orphanTests) {
        if (orphans > 0 || orphanTests > 0 || relationships >= HIGH_IMPACT_RELATIONSHIPS) {
            return ImpactLevel.HIGH;
        }
        if (relationships > 0 || tests > 0) {
            return ImpactLevel.MEDIUM;
        }
        return ImpactLevel.LOW;
    }

    /**
     * RF15: exclui o elemento e suas associacoes (relacoes e vinculos com testes), tudo como soft delete
     * e na mesma transacao. O codigo do elemento nunca e reutilizado.
     */
    @Transactional
    public void delete(UUID projectId, UUID elementId, UUID userId) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Element element = find(projectId, elementId);
        relationshipRepository.softDeleteAllOfElement(elementId);
        coverageRepository.softDeleteCoverageOf(elementId);
        elementRepository.delete(element);
        projectRepository.touch(projectId);
    }

    private Element find(UUID projectId, UUID elementId) {
        return elementRepository.findByIdAndProjectId(elementId, projectId)
                .orElseThrow(() -> new ElementNotFoundException(elementId));
    }
}
