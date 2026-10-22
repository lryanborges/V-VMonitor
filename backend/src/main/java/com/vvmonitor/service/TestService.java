package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.TestRequest;
import com.vvmonitor.api.dto.response.ElementSummary;
import com.vvmonitor.api.dto.response.TestResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.TestCase;
import com.vvmonitor.domain.entity.TestCoverage;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.SequenceKind;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.TestNotFoundException;
import com.vvmonitor.infra.repository.CodeSequenceRepository;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.GraphPositionRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.TestCaseRepository;
import com.vvmonitor.infra.repository.TestCoverageRepository;
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

/**
 * Testes e a cobertura dos requisitos (RF9, RF13). Todo teste cobre ao menos um requisito (RF ou RNF) ao ser
 * criado ou editado; regras de negocio nao sao cobertas. Um teste so fica sem requisito quando o ultimo
 * requisito que ele cobria e excluido.
 */
@Service
public class TestService {

    private static final Comparator<ElementSummary> BY_CODE =
            Comparator.comparing(ElementSummary::kind).thenComparing(e -> codeNumber(e.code()));

    private final TestCaseRepository testRepository;
    private final TestCoverageRepository coverageRepository;
    private final ElementRepository elementRepository;
    private final CodeSequenceRepository codeSequenceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;
    private final VersionService versionService;
    private final GraphPositionRepository positionRepository;

    public TestService(TestCaseRepository testRepository, TestCoverageRepository coverageRepository,
                       ElementRepository elementRepository, CodeSequenceRepository codeSequenceRepository,
                       ProjectRepository projectRepository, ProjectAccessService accessService,
                       VersionService versionService, GraphPositionRepository positionRepository) {
        this.testRepository = testRepository;
        this.coverageRepository = coverageRepository;
        this.elementRepository = elementRepository;
        this.codeSequenceRepository = codeSequenceRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.versionService = versionService;
        this.positionRepository = positionRepository;
    }

    /** Testes do projeto em ordem de codigo, com os requisitos que cada um cobre. */
    @Transactional(readOnly = true)
    public List<TestResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Map<UUID, Element> elements = elementsOf(projectId);
        Map<UUID, List<UUID>> covered = coverageRepository.findAllOfProject(projectId).stream()
                .collect(Collectors.groupingBy(TestCoverage::getTestId,
                        Collectors.mapping(TestCoverage::getElementId, Collectors.toList())));
        Set<UUID> pending = versionService.pendingIds(projectId);
        return testRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(TestCase::codeNumber))
                .map(t -> TestResponse.from(t, summaries(covered.getOrDefault(t.getId(), List.of()), elements),
                        pending.contains(t.getId())))
                .toList();
    }

    /** RF9: cadastra o teste com codigo T gerado e os requisitos que ele cobre. */
    @Transactional
    public TestResponse create(UUID projectId, UUID userId, TestRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Map<UUID, Element> elements = elementsOf(projectId);
        Set<UUID> requirementIds = validRequirements(request.elementIds(), elements);
        int number = codeSequenceRepository.allocate(projectId, SequenceKind.TEST);
        TestCase test = testRepository.saveAndFlush(new TestCase(projectId, number, request.description(), userId));
        requirementIds.forEach(id -> coverageRepository.save(new TestCoverage(test.getId(), id)));
        coverageRepository.flush();
        projectRepository.touch(projectId);
        return TestResponse.from(test, summaries(List.copyOf(requirementIds), elements), true);
    }

    /** RF9/RF14: troca a descricao e os requisitos cobertos (os vinculos que sairam sao removidos). */
    @Transactional
    public TestResponse update(UUID projectId, UUID testId, UUID userId, TestRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        TestCase test = find(projectId, testId);
        Map<UUID, Element> elements = elementsOf(projectId);
        Set<UUID> requirementIds = validRequirements(request.elementIds(), elements);
        test.update(request.description());
        testRepository.saveAndFlush(test);
        List<TestCoverage> current = coverageRepository.findAllByTestId(testId);
        current.stream().filter(c -> !requirementIds.contains(c.getElementId())).forEach(coverageRepository::delete);
        Set<UUID> kept = current.stream().map(TestCoverage::getElementId).collect(Collectors.toSet());
        requirementIds.stream().filter(id -> !kept.contains(id))
                .forEach(id -> coverageRepository.save(new TestCoverage(testId, id)));
        coverageRepository.flush();
        projectRepository.touch(projectId);
        return TestResponse.from(test, summaries(List.copyOf(requirementIds), elements),
                versionService.pendingIds(projectId).contains(testId));
    }

    /** Exclui (soft delete) o teste e seus vinculos; os requisitos continuam no modelo. */
    @Transactional
    public void delete(UUID projectId, UUID testId, UUID userId) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        TestCase test = find(projectId, testId);
        coverageRepository.findAllByTestId(testId).forEach(coverageRepository::delete);
        testRepository.delete(test);
        // RF21: a posicao do teste no grafo sai com ele
        positionRepository.deleteNodes(List.of(testId));
        projectRepository.touch(projectId);
    }

    private TestCase find(UUID projectId, UUID testId) {
        return testRepository.findByIdAndProjectId(testId, projectId)
                .orElseThrow(() -> new TestNotFoundException(testId));
    }

    private Map<UUID, Element> elementsOf(UUID projectId) {
        return elementRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(Element::getId, Function.identity()));
    }

    /** Ids informados sem repeticao; todos precisam ser requisitos (RF/RNF) do projeto. */
    private static Set<UUID> validRequirements(List<UUID> ids, Map<UUID, Element> elements) {
        Set<UUID> unique = new LinkedHashSet<>(ids);
        for (UUID id : unique) {
            Element element = elements.get(id);
            if (element == null) {
                throw new InvalidFieldException("elementIds", "Requisito não encontrado neste projeto.");
            }
            if (!element.getKind().isRequirement()) {
                throw new InvalidFieldException("elementIds",
                        "Testes cobrem requisitos funcionais e não funcionais, não regras de negócio (" + element.getCode() + ").");
            }
        }
        return unique;
    }

    /** Requisitos cobertos em ordem (RF antes de RNF, RF2 antes de RF10); ignora os que nao existem mais. */
    private static List<ElementSummary> summaries(List<UUID> ids, Map<UUID, Element> elements) {
        return ids.stream().map(elements::get).filter(e -> e != null).map(ElementSummary::from).sorted(BY_CODE).toList();
    }

    private static int codeNumber(String code) {
        return Integer.parseInt(code.replaceAll("\\D", ""));
    }
}
