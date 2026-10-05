package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateRelationshipRequest;
import com.vvmonitor.api.dto.request.CreateRelationshipTypeRequest;
import com.vvmonitor.api.dto.request.UpdateRelationshipRequest;
import com.vvmonitor.api.dto.response.RelationshipResponse;
import com.vvmonitor.api.dto.response.RelationshipTypeResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.DuplicateRelationshipException;
import com.vvmonitor.domain.exception.ElementNotFoundException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.RelationshipNotFoundException;
import com.vvmonitor.domain.exception.RelationshipTypeAlreadyExistsException;
import com.vvmonitor.domain.exception.RelationshipTypeNotFoundException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.RelationshipTypeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Relacionamentos entre requisitos e regras de negocio (RF7) e seus tipos (RF8), UC-06. */
@Service
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final RelationshipTypeRepository typeRepository;
    private final ElementRepository elementRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;

    public RelationshipService(RelationshipRepository relationshipRepository, RelationshipTypeRepository typeRepository,
                               ElementRepository elementRepository, ProjectRepository projectRepository,
                               ProjectAccessService accessService) {
        this.relationshipRepository = relationshipRepository;
        this.typeRepository = typeRepository;
        this.elementRepository = elementRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
    }

    /** UC-06 passo 4: pre-definidos (Dependencia, Conflito, Refinamento, Similaridade) e os do projeto. */
    @Transactional(readOnly = true)
    public List<RelationshipTypeResponse> listTypes(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        return typeRepository.findAvailableFor(projectId).stream().map(RelationshipTypeResponse::from).toList();
    }

    /** UC-06, sequencia alternativa, passos 7 e 8: registra um novo tipo de relacionamento no projeto. */
    @Transactional
    public RelationshipTypeResponse createType(UUID projectId, UUID userId, CreateRelationshipTypeRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        String name = request.name().strip();
        if (typeRepository.existsNameFor(projectId, name)) {
            throw new RelationshipTypeAlreadyExistsException(name);
        }
        try {
            return RelationshipTypeResponse.from(typeRepository.saveAndFlush(RelationshipType.custom(projectId, name)));
        } catch (DataIntegrityViolationException e) {
            // corrida entre dois cadastros com o mesmo nome; o indice unico do banco barra o segundo
            throw new RelationshipTypeAlreadyExistsException(name);
        }
    }

    /** Todas as relacoes do projeto, com as duas pontas, em ordem de criacao. */
    @Transactional(readOnly = true)
    public List<RelationshipResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        Map<UUID, Element> elements = elementRepository.findAllByProjectId(projectId).stream()
                .collect(Collectors.toMap(Element::getId, Function.identity()));
        Map<UUID, RelationshipType> types = typeRepository.findAvailableFor(projectId).stream()
                .collect(Collectors.toMap(RelationshipType::getId, Function.identity()));

        return relationshipRepository.findAllByProjectIdOrderByCreatedAt(projectId).stream()
                // relacao cuja ponta foi removida nao e exibida (a remocao em cascata chega no RF15)
                .filter(r -> elements.containsKey(r.getSourceId()) && elements.containsKey(r.getTargetId()))
                .filter(r -> types.containsKey(r.getTypeId()))
                .map(r -> RelationshipResponse.from(r, types.get(r.getTypeId()),
                        elements.get(r.getSourceId()), elements.get(r.getTargetId())))
                .toList();
    }

    /** RF7 e RF8, UC-06 passo 6: registra e vincula os dois elementos. */
    @Transactional
    public RelationshipResponse create(UUID projectId, UUID userId, CreateRelationshipRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        if (request.sourceId().equals(request.targetId())) {
            throw new InvalidFieldException("targetId", "Um elemento não pode se relacionar com ele mesmo.");
        }
        Element source = findElement(projectId, request.sourceId());
        Element target = findElement(projectId, request.targetId());
        RelationshipType type = findType(projectId, request.typeId());

        Element first = canonicalSource(type, source, target);
        Element second = first == source ? target : source;

        String sentence = sentence(source, type, target);
        if (relationshipRepository.existsBySourceIdAndTargetIdAndTypeId(first.getId(), second.getId(), type.getId())) {
            throw new DuplicateRelationshipException(sentence);
        }
        Relationship relationship;
        try {
            relationship = relationshipRepository.saveAndFlush(
                    new Relationship(projectId, first.getId(), second.getId(), type.getId(), userId));
        } catch (DataIntegrityViolationException e) {
            // corrida entre dois cadastros iguais; o indice unico do banco barra o segundo
            throw new DuplicateRelationshipException(sentence);
        }
        projectRepository.touch(projectId);
        return RelationshipResponse.from(relationship, type, first, second);
    }

    /**
     * RF14: troca o tipo e, se pedido, inverte a direcao. As pontas continuam as mesmas; a relacao volta a
     * ser rascunho, como o elemento editado.
     */
    @Transactional
    public RelationshipResponse update(UUID projectId, UUID relationshipId, UUID userId,
                                       UpdateRelationshipRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Relationship relationship = findRelationship(projectId, relationshipId);
        RelationshipType type = findType(projectId, request.typeId());
        Element source = findElement(projectId, relationship.getSourceId());
        Element target = findElement(projectId, relationship.getTargetId());
        if (request.reversed()) {
            Element swap = source;
            source = target;
            target = swap;
        }

        Element first = canonicalSource(type, source, target);
        Element second = first == source ? target : source;
        String sentence = sentence(source, type, target);
        if (relationshipRepository.existsBySourceIdAndTargetIdAndTypeIdAndIdNot(
                first.getId(), second.getId(), type.getId(), relationship.getId())) {
            throw new DuplicateRelationshipException(sentence);
        }
        relationship.update(type.getId(), first.getId(), second.getId());
        try {
            relationshipRepository.saveAndFlush(relationship);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRelationshipException(sentence);
        }
        projectRepository.touch(projectId);
        return RelationshipResponse.from(relationship, type, first, second);
    }

    /** RF15: remove (soft delete) uma relacao; os dois elementos continuam no modelo. */
    @Transactional
    public void delete(UUID projectId, UUID relationshipId, UUID userId) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        relationshipRepository.delete(findRelationship(projectId, relationshipId));
        projectRepository.touch(projectId);
    }

    /** Em tipos simetricos A-B e B-A sao a mesma relacao: grava sempre na mesma ordem (menor UUID primeiro). */
    private static Element canonicalSource(RelationshipType type, Element source, Element target) {
        return type.isSymmetric() && source.getId().compareTo(target.getId()) > 0 ? target : source;
    }

    private RelationshipType findType(UUID projectId, UUID typeId) {
        return typeRepository.findById(typeId)
                .filter(t -> t.isAvailableIn(projectId))
                .orElseThrow(() -> new RelationshipTypeNotFoundException(typeId));
    }

    private Relationship findRelationship(UUID projectId, UUID relationshipId) {
        return relationshipRepository.findByIdAndProjectId(relationshipId, projectId)
                .orElseThrow(() -> new RelationshipNotFoundException(relationshipId));
    }

    private Element findElement(UUID projectId, UUID elementId) {
        return elementRepository.findByIdAndProjectId(elementId, projectId)
                .orElseThrow(() -> new ElementNotFoundException(elementId));
    }

    /** Frase da excecao de duplicidade, ex.: "RF7 já depende de RF5." */
    static String sentence(Element source, RelationshipType type, Element target) {
        String s = source.getCode();
        String t = target.getCode();
        return switch (type.isCustom() ? "" : type.getName()) {
            case "Dependência" -> s + " já depende de " + t + ".";
            case "Refinamento" -> s + " já refina " + t + ".";
            case "Conflito" -> s + " já está em conflito com " + t + ".";
            case "Similaridade" -> s + " já é similar a " + t + ".";
            default -> s + " já tem o relacionamento “" + type.getName() + "” com " + t + ".";
        };
    }
}
