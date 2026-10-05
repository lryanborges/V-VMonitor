package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateRelationshipRequest;
import com.vvmonitor.api.dto.request.CreateRelationshipTypeRequest;
import com.vvmonitor.api.dto.request.UpdateRelationshipRequest;
import com.vvmonitor.api.dto.response.RelationshipResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.domain.exception.DuplicateRelationshipException;
import com.vvmonitor.domain.exception.ElementNotFoundException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.RelationshipNotFoundException;
import com.vvmonitor.domain.exception.RelationshipTypeAlreadyExistsException;
import com.vvmonitor.domain.exception.RelationshipTypeNotFoundException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.RelationshipTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelationshipServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private RelationshipRepository relationshipRepository;
    private RelationshipTypeRepository typeRepository;
    private ElementRepository elementRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private RelationshipService service;

    private Element rf7;
    private Element rf5;
    private RelationshipType dependencia;
    private RelationshipType conflito;

    @BeforeEach
    void setUp() {
        relationshipRepository = mock(RelationshipRepository.class);
        typeRepository = mock(RelationshipTypeRepository.class);
        elementRepository = mock(ElementRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        service = new RelationshipService(relationshipRepository, typeRepository, elementRepository,
                projectRepository, new ProjectAccessService(memberRepository));

        when(memberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(elementRepository.findByIdAndProjectId(any(), any())).thenReturn(Optional.empty());
        when(relationshipRepository.saveAndFlush(any(Relationship.class))).thenAnswer(inv -> inv.getArgument(0));

        rf7 = element("RF7");
        rf5 = element("RF5");
        dependencia = predefined("Dependência", false);
        conflito = predefined("Conflito", true);
    }

    private Element element(String code) {
        Element e = new Element(projectId, ElementKind.FUNCTIONAL, code, "desc " + code, Priority.MANDATORY, userId);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        when(elementRepository.findByIdAndProjectId(e.getId(), projectId)).thenReturn(Optional.of(e));
        return e;
    }

    private RelationshipType predefined(String name, boolean symmetric) {
        RelationshipType t = RelationshipType.custom(null, name);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(t, "symmetric", symmetric);
        when(typeRepository.findById(t.getId())).thenReturn(Optional.of(t));
        return t;
    }

    private void loggedAs(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, role)));
    }

    @Test
    void criaRelacaoComoRascunhoNaOrdemPedida() {
        loggedAs(MemberRole.EDITOR);

        RelationshipResponse response = service.create(projectId, userId,
                new CreateRelationshipRequest(rf7.getId(), rf5.getId(), dependencia.getId()));

        assertThat(response.source().code()).isEqualTo("RF7");
        assertThat(response.target().code()).isEqualTo("RF5");
        assertThat(response.type().name()).isEqualTo("Dependência");
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.DRAFT);
        verify(projectRepository).touch(projectId);
    }

    @Test
    void relacaoDuplicadaEhBarradaComFrase() {
        loggedAs(MemberRole.EDITOR);
        when(relationshipRepository.existsBySourceIdAndTargetIdAndTypeId(rf7.getId(), rf5.getId(), dependencia.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(projectId, userId,
                new CreateRelationshipRequest(rf7.getId(), rf5.getId(), dependencia.getId())))
                .isInstanceOf(DuplicateRelationshipException.class)
                .hasMessage("RF7 já depende de RF5. Registro duplicado não é permitido.");
        verify(relationshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void tipoSimetricoGravaSempreNaMesmaOrdem() {
        loggedAs(MemberRole.EDITOR);
        ArgumentCaptor<Relationship> saved = ArgumentCaptor.forClass(Relationship.class);

        service.create(projectId, userId, new CreateRelationshipRequest(rf7.getId(), rf5.getId(), conflito.getId()));
        service.create(projectId, userId, new CreateRelationshipRequest(rf5.getId(), rf7.getId(), conflito.getId()));

        verify(relationshipRepository, org.mockito.Mockito.times(2)).saveAndFlush(saved.capture());
        Relationship a = saved.getAllValues().get(0);
        Relationship b = saved.getAllValues().get(1);
        assertThat(a.getSourceId()).isEqualTo(b.getSourceId());
        assertThat(a.getTargetId()).isEqualTo(b.getTargetId());
    }

    @Test
    void tipoDirigidoMantemAsDuasDirecoesDistintas() {
        loggedAs(MemberRole.EDITOR);
        ArgumentCaptor<Relationship> saved = ArgumentCaptor.forClass(Relationship.class);

        service.create(projectId, userId, new CreateRelationshipRequest(rf7.getId(), rf5.getId(), dependencia.getId()));
        service.create(projectId, userId, new CreateRelationshipRequest(rf5.getId(), rf7.getId(), dependencia.getId()));

        verify(relationshipRepository, org.mockito.Mockito.times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues().get(0).getSourceId()).isEqualTo(rf7.getId());
        assertThat(saved.getAllValues().get(1).getSourceId()).isEqualTo(rf5.getId());
    }

    @Test
    void elementoComEleMesmoEhRejeitado() {
        loggedAs(MemberRole.EDITOR);

        assertThatThrownBy(() -> service.create(projectId, userId,
                new CreateRelationshipRequest(rf7.getId(), rf7.getId(), dependencia.getId())))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("field").isEqualTo("targetId");
    }

    @Test
    void elementoDeOutroProjetoNaoEhEncontrado() {
        loggedAs(MemberRole.EDITOR);

        assertThatThrownBy(() -> service.create(projectId, userId,
                new CreateRelationshipRequest(rf7.getId(), UUID.randomUUID(), dependencia.getId())))
                .isInstanceOf(ElementNotFoundException.class);
    }

    @Test
    void tipoPersonalizadoDeOutroProjetoNaoEhEncontrado() {
        loggedAs(MemberRole.EDITOR);
        RelationshipType alheio = RelationshipType.custom(UUID.randomUUID(), "Valida");
        ReflectionTestUtils.setField(alheio, "id", UUID.randomUUID());
        when(typeRepository.findById(alheio.getId())).thenReturn(Optional.of(alheio));

        assertThatThrownBy(() -> service.create(projectId, userId,
                new CreateRelationshipRequest(rf7.getId(), rf5.getId(), alheio.getId())))
                .isInstanceOf(RelationshipTypeNotFoundException.class);
    }

    @Test
    void visualizadorNaoRelaciona() {
        loggedAs(MemberRole.VIEWER);

        assertThatThrownBy(() -> service.create(projectId, userId,
                new CreateRelationshipRequest(rf7.getId(), rf5.getId(), dependencia.getId())))
                .isInstanceOf(ProjectAccessDeniedException.class);
    }

    @Test
    void novoTipoPersonalizadoEhDirigidoEDoProjeto() {
        loggedAs(MemberRole.EDITOR);
        when(typeRepository.saveAndFlush(any(RelationshipType.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createType(projectId, userId, new CreateRelationshipTypeRequest("  Valida  "));

        assertThat(response.name()).isEqualTo("Valida");
        assertThat(response.custom()).isTrue();
        assertThat(response.symmetric()).isFalse();
    }

    @Test
    void tipoComNomeRepetidoEhBarrado() {
        loggedAs(MemberRole.EDITOR);
        when(typeRepository.existsNameFor(projectId, "conflito")).thenReturn(true);

        assertThatThrownBy(() -> service.createType(projectId, userId, new CreateRelationshipTypeRequest("conflito")))
                .isInstanceOf(RelationshipTypeAlreadyExistsException.class);
        verify(typeRepository, never()).saveAndFlush(any());
    }

    @Test
    void listagemIgnoraRelacaoComPontaRemovida() {
        loggedAs(MemberRole.VIEWER);
        Relationship ok = new Relationship(projectId, rf7.getId(), rf5.getId(), dependencia.getId(), userId);
        Relationship orfa = new Relationship(projectId, rf7.getId(), UUID.randomUUID(), dependencia.getId(), userId);
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rf7, rf5));
        when(typeRepository.findAvailableFor(projectId)).thenReturn(List.of(dependencia, conflito));
        when(relationshipRepository.findAllByProjectIdOrderByCreatedAt(projectId)).thenReturn(List.of(ok, orfa));

        assertThat(service.list(projectId, userId)).extracting(r -> r.source().code() + "->" + r.target().code())
                .containsExactly("RF7->RF5");
    }

    @Test
    void frasesDosTipos() {
        RelationshipType refinamento = predefined("Refinamento", false);
        RelationshipType similaridade = predefined("Similaridade", true);
        RelationshipType valida = RelationshipType.custom(projectId, "Valida");

        assertThat(RelationshipService.sentence(rf7, refinamento, rf5)).isEqualTo("RF7 já refina RF5.");
        assertThat(RelationshipService.sentence(rf7, conflito, rf5)).isEqualTo("RF7 já está em conflito com RF5.");
        assertThat(RelationshipService.sentence(rf7, similaridade, rf5)).isEqualTo("RF7 já é similar a RF5.");
        assertThat(RelationshipService.sentence(rf7, valida, rf5)).isEqualTo("RF7 já tem o relacionamento “Valida” com RF5.");
    }

    private Relationship existing(Element source, RelationshipType type, Element target) {
        Relationship r = new Relationship(projectId, source.getId(), target.getId(), type.getId(), userId);
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(r, "submissionStatus", SubmissionStatus.SUBMITTED);
        when(relationshipRepository.findByIdAndProjectId(r.getId(), projectId)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void edicaoTrocaTipoEVoltaParaRascunho() {
        loggedAs(MemberRole.EDITOR);
        RelationshipType refinamento = predefined("Refinamento", false);
        Relationship r = existing(rf7, dependencia, rf5);

        RelationshipResponse response = service.update(projectId, r.getId(), userId,
                new UpdateRelationshipRequest(refinamento.getId(), false));

        assertThat(response.type().name()).isEqualTo("Refinamento");
        assertThat(response.source().code()).isEqualTo("RF7");
        assertThat(r.getSubmissionStatus()).isEqualTo(SubmissionStatus.DRAFT);
        verify(projectRepository).touch(projectId);
    }

    @Test
    void edicaoInverteADirecao() {
        loggedAs(MemberRole.EDITOR);
        Relationship r = existing(rf7, dependencia, rf5);

        RelationshipResponse response = service.update(projectId, r.getId(), userId,
                new UpdateRelationshipRequest(dependencia.getId(), true));

        assertThat(response.source().code()).isEqualTo("RF5");
        assertThat(response.target().code()).isEqualTo("RF7");
        assertThat(r.getSourceId()).isEqualTo(rf5.getId());
    }

    @Test
    void edicaoQueGeraDuplicataEhBarrada() {
        loggedAs(MemberRole.EDITOR);
        Relationship r = existing(rf7, dependencia, rf5);
        when(relationshipRepository.existsBySourceIdAndTargetIdAndTypeIdAndIdNot(
                rf5.getId(), rf7.getId(), dependencia.getId(), r.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(projectId, r.getId(), userId,
                new UpdateRelationshipRequest(dependencia.getId(), true)))
                .isInstanceOf(DuplicateRelationshipException.class)
                .hasMessageStartingWith("RF5 já depende de RF7.");
    }

    @Test
    void removeRelacao() {
        loggedAs(MemberRole.EDITOR);
        Relationship r = existing(rf7, dependencia, rf5);

        service.delete(projectId, r.getId(), userId);

        verify(relationshipRepository).delete(r);
        verify(projectRepository).touch(projectId);
    }

    @Test
    void relacaoDeOutroProjetoNaoEhEncontrada() {
        loggedAs(MemberRole.EDITOR);

        assertThatThrownBy(() -> service.delete(projectId, UUID.randomUUID(), userId))
                .isInstanceOf(RelationshipNotFoundException.class);
    }
}
