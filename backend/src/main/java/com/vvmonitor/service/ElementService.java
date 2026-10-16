package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateElementRequest;
import com.vvmonitor.api.dto.request.UpdateElementRequest;
import com.vvmonitor.api.dto.response.ElementResponse;
import com.vvmonitor.api.dto.response.NextCodeResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
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
import java.util.Set;
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
    private final VersionService versionService;

    public ElementService(ElementRepository elementRepository, ElementStatsRepository statsRepository,
                          CodeSequenceRepository codeSequenceRepository, ProjectRepository projectRepository,
                          ProjectAccessService accessService, VersionService versionService) {
        this.elementRepository = elementRepository;
        this.statsRepository = statsRepository;
        this.codeSequenceRepository = codeSequenceRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.versionService = versionService;
    }

    /** Requisitos e regras de negocio do projeto, inclusive os pendentes (diferentes da ultima versao). */
    @Transactional(readOnly = true)
    public List<ElementResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Map<UUID, ElementStats> stats = statsRepository.findByProjectId(projectId);
        Set<UUID> pending = versionService.pendingIds(projectId);
        return elementRepository.findAllByProjectId(projectId).stream()
                .sorted(BY_KIND_AND_NUMBER)
                .map(e -> ElementResponse.from(e, stats.getOrDefault(e.getId(), ElementStats.NONE), pending.contains(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ElementResponse findById(UUID projectId, UUID elementId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Element element = elementRepository.findByIdAndProjectId(elementId, projectId)
                .orElseThrow(() -> new ElementNotFoundException(elementId));
        ElementStats stats = statsRepository.findByProjectId(projectId).getOrDefault(elementId, ElementStats.NONE);
        return ElementResponse.from(element, stats, versionService.pendingIds(projectId).contains(elementId));
    }

    /**
     * RF5 e RF6, UC-05 e UC-07: valida, reserva o codigo e armazena o elemento (pendente ate a proxima versao).
     * Tudo na mesma transacao: se o armazenamento falhar, o numero reservado tambem e desfeito.
     */
    @Transactional
    public ElementResponse create(UUID projectId, UUID userId, CreateElementRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        validatePriority(request.kind(), request.priority());

        ElementKind kind = request.kind();
        int number = codeSequenceRepository.allocate(projectId, kind.sequenceKind());
        Element element = elementRepository.saveAndFlush(new Element(
                projectId, kind, kind.code(number), request.description(), request.priority(), userId));
        projectRepository.touch(projectId);
        return ElementResponse.from(element, ElementStats.NONE, true);
    }

    /** Previa do proximo codigo do tipo, exibida no formulario de novo elemento. */
    @Transactional(readOnly = true)
    public NextCodeResponse nextCode(UUID projectId, UUID userId, ElementKind kind) {
        accessService.requireMember(projectId, userId);
        return new NextCodeResponse(kind, kind.code(codeSequenceRepository.peek(projectId, kind.sequenceKind())));
    }

    /**
     * RF14: edita descricao e prioridade; tipo e codigo nao mudam. A edicao vale na hora e fica pendente
     * ate a proxima versao (deixa de ficar se voltar ao conteudo da ultima versao).
     */
    @Transactional
    public ElementResponse update(UUID projectId, UUID elementId, UUID userId, UpdateElementRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Element element = elementRepository.findByIdAndProjectId(elementId, projectId)
                .orElseThrow(() -> new ElementNotFoundException(elementId));
        validatePriority(element.getKind(), request.priority());

        element.update(request.description(), request.priority());
        elementRepository.saveAndFlush(element);
        projectRepository.touch(projectId);
        ElementStats stats = statsRepository.findByProjectId(projectId).getOrDefault(elementId, ElementStats.NONE);
        return ElementResponse.from(element, stats, versionService.pendingIds(projectId).contains(elementId));
    }

    /** Requisitos exigem prioridade (RF5); regras de negocio nao possuem (RF6). */
    private static void validatePriority(ElementKind kind, Priority priority) {
        if (kind.isRequirement() && priority == null) {
            throw new InvalidFieldException("priority", "A prioridade é obrigatória para requisitos.");
        }
        if (!kind.isRequirement() && priority != null) {
            throw new InvalidFieldException("priority", "Regras de negócio não têm prioridade.");
        }
    }
}
