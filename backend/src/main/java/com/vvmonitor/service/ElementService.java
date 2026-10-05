package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateElementRequest;
import com.vvmonitor.api.dto.response.ElementResponse;
import com.vvmonitor.api.dto.response.NextCodeResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.ElementNotFoundException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.infra.repository.CodeSequenceRepository;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository.ElementStats;
import com.vvmonitor.infra.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ElementService {

    /** Ordem da lista: por tipo (RF, RNF, RN) e pelo numero do codigo, para RF2 vir antes de RF10. */
    private static final Comparator<Element> BY_KIND_AND_NUMBER =
            Comparator.comparing(Element::getKind).thenComparingInt(Element::codeNumber);

    private final ElementRepository elementRepository;
    private final ElementStatsRepository statsRepository;
    private final CodeSequenceRepository codeSequenceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;

    public ElementService(ElementRepository elementRepository, ElementStatsRepository statsRepository,
                          CodeSequenceRepository codeSequenceRepository, ProjectRepository projectRepository,
                          ProjectAccessService accessService) {
        this.elementRepository = elementRepository;
        this.statsRepository = statsRepository;
        this.codeSequenceRepository = codeSequenceRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
    }

    /** Requisitos e regras de negocio do projeto, rascunhos inclusive. */
    @Transactional(readOnly = true)
    public List<ElementResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Map<UUID, ElementStats> stats = statsRepository.findByProjectId(projectId);
        return elementRepository.findAllByProjectId(projectId).stream()
                .sorted(BY_KIND_AND_NUMBER)
                .map(e -> ElementResponse.from(e, stats.getOrDefault(e.getId(), ElementStats.NONE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ElementResponse findById(UUID projectId, UUID elementId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Element element = elementRepository.findByIdAndProjectId(elementId, projectId)
                .orElseThrow(() -> new ElementNotFoundException(elementId));
        ElementStats stats = statsRepository.findByProjectId(projectId).getOrDefault(elementId, ElementStats.NONE);
        return ElementResponse.from(element, stats);
    }

    /**
     * RF5 e RF6, UC-05 e UC-07: valida, reserva o codigo e armazena o elemento como rascunho.
     * Tudo na mesma transacao: se o armazenamento falhar, o numero reservado tambem e desfeito.
     */
    @Transactional
    public ElementResponse create(UUID projectId, UUID userId, CreateElementRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        validatePriority(request);

        ElementKind kind = request.kind();
        int number = codeSequenceRepository.allocate(projectId, kind.sequenceKind());
        Element element = elementRepository.saveAndFlush(new Element(
                projectId, kind, kind.code(number), request.description(), request.priority(), userId));
        projectRepository.touch(projectId);
        return ElementResponse.from(element, ElementStats.NONE);
    }

    /** Previa do proximo codigo do tipo, exibida no formulario de novo elemento. */
    @Transactional(readOnly = true)
    public NextCodeResponse nextCode(UUID projectId, UUID userId, ElementKind kind) {
        accessService.requireMember(projectId, userId);
        return new NextCodeResponse(kind, kind.code(codeSequenceRepository.peek(projectId, kind.sequenceKind())));
    }

    /** Requisitos exigem prioridade (RF5); regras de negocio nao possuem (RF6). */
    private static void validatePriority(CreateElementRequest request) {
        if (request.kind().isRequirement() && request.priority() == null) {
            throw new InvalidFieldException("priority", "A prioridade é obrigatória para requisitos.");
        }
        if (!request.kind().isRequirement() && request.priority() != null) {
            throw new InvalidFieldException("priority", "Regras de negócio não têm prioridade.");
        }
    }
}
