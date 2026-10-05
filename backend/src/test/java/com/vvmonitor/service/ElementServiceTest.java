package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateElementRequest;
import com.vvmonitor.api.dto.request.UpdateElementRequest;
import com.vvmonitor.api.dto.response.ElementResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.enums.SequenceKind;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.domain.exception.ElementNotFoundException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.CodeSequenceRepository;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository.ElementStats;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElementServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private ElementRepository elementRepository;
    private ElementStatsRepository statsRepository;
    private CodeSequenceRepository codeSequenceRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private ElementService elementService;

    @BeforeEach
    void setUp() {
        elementRepository = mock(ElementRepository.class);
        statsRepository = mock(ElementStatsRepository.class);
        codeSequenceRepository = mock(CodeSequenceRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        elementService = new ElementService(elementRepository, statsRepository, codeSequenceRepository,
                projectRepository, new ProjectAccessService(memberRepository));

        when(memberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(elementRepository.saveAndFlush(any(Element.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void loggedAs(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, role)));
    }

    private Element element(ElementKind kind, String code) {
        Element e = new Element(projectId, kind, code, "d",
                kind.isRequirement() ? Priority.MANDATORY : null, userId);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        return e;
    }

    @Test
    void requisitoRecebeCodigoReservadoEEntraComoRascunho() {
        loggedAs(MemberRole.EDITOR);
        when(codeSequenceRepository.allocate(projectId, SequenceKind.FUNCTIONAL)).thenReturn(21);

        ElementResponse response = elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.FUNCTIONAL, "  Exportar matriz em CSV. ", Priority.DESIRABLE));

        assertThat(response.code()).isEqualTo("RF21");
        assertThat(response.description()).isEqualTo("Exportar matriz em CSV.");
        assertThat(response.priority()).isEqualTo(Priority.DESIRABLE);
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.DRAFT);
        verify(projectRepository).touch(projectId);
    }

    @Test
    void cadaTipoUsaSeuPrefixoEContador() {
        loggedAs(MemberRole.OWNER);
        when(codeSequenceRepository.allocate(projectId, SequenceKind.NON_FUNCTIONAL)).thenReturn(3);
        when(codeSequenceRepository.allocate(projectId, SequenceKind.BUSINESS_RULE)).thenReturn(5);

        assertThat(elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.NON_FUNCTIONAL, "x", Priority.OPTIONAL)).code()).isEqualTo("RNF3");
        assertThat(elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.BUSINESS_RULE, "x", null)).code()).isEqualTo("RN5");
    }

    @Test
    void requisitoSemPrioridadeEhRejeitadoSemReservarCodigo() {
        loggedAs(MemberRole.EDITOR);

        assertThatThrownBy(() -> elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.FUNCTIONAL, "x", null)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("A prioridade é obrigatória para requisitos.")
                .extracting("field").isEqualTo("priority");
        verify(codeSequenceRepository, never()).allocate(any(), any());
    }

    @Test
    void regraDeNegocioComPrioridadeEhRejeitada() {
        loggedAs(MemberRole.EDITOR);

        assertThatThrownBy(() -> elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.BUSINESS_RULE, "x", Priority.MANDATORY)))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Regras de negócio não têm prioridade.");
    }

    @Test
    void visualizadorNaoCadastra() {
        loggedAs(MemberRole.VIEWER);

        assertThatThrownBy(() -> elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.FUNCTIONAL, "x", Priority.MANDATORY)))
                .isInstanceOf(ProjectAccessDeniedException.class);
        verify(elementRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoMembroNaoListaNemCadastra() {
        assertThatThrownBy(() -> elementService.list(projectId, userId))
                .isInstanceOf(ProjectNotFoundException.class);
        assertThatThrownBy(() -> elementService.create(projectId, userId,
                new CreateElementRequest(ElementKind.FUNCTIONAL, "x", Priority.MANDATORY)))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void listaOrdenadaPorTipoENumeroComContagens() {
        loggedAs(MemberRole.VIEWER);
        Element rf10 = element(ElementKind.FUNCTIONAL, "RF10");
        Element rf2 = element(ElementKind.FUNCTIONAL, "RF2");
        Element rn1 = element(ElementKind.BUSINESS_RULE, "RN1");
        Element rnf1 = element(ElementKind.NON_FUNCTIONAL, "RNF1");
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rn1, rf10, rnf1, rf2));
        when(statsRepository.findByProjectId(projectId)).thenReturn(Map.of(rf2.getId(), new ElementStats(6, 1)));

        List<ElementResponse> list = elementService.list(projectId, userId);

        assertThat(list).extracting(ElementResponse::code).containsExactly("RF2", "RF10", "RNF1", "RN1");
        assertThat(list.getFirst().links()).isEqualTo(6);
        assertThat(list.getFirst().tests()).isEqualTo(1);
        assertThat(list.get(1).links()).isZero();
    }

    @Test
    void elementoDeOutroProjetoNaoEhEncontrado() {
        loggedAs(MemberRole.VIEWER);
        UUID elementId = UUID.randomUUID();
        when(elementRepository.findByIdAndProjectId(elementId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> elementService.findById(projectId, elementId, userId))
                .isInstanceOf(ElementNotFoundException.class);
    }

    @Test
    void previaDoProximoCodigo() {
        loggedAs(MemberRole.VIEWER);
        when(codeSequenceRepository.peek(projectId, SequenceKind.FUNCTIONAL)).thenReturn(21);

        assertThat(elementService.nextCode(projectId, userId, ElementKind.FUNCTIONAL).code()).isEqualTo("RF21");
        verify(codeSequenceRepository, never()).allocate(any(), any());
    }

    @Test
    void edicaoAtualizaDescricaoEPrioridadeEVoltaParaRascunho() {
        loggedAs(MemberRole.EDITOR);
        Element rf2 = element(ElementKind.FUNCTIONAL, "RF2");
        ReflectionTestUtils.setField(rf2, "submissionStatus", SubmissionStatus.SUBMITTED);
        when(elementRepository.findByIdAndProjectId(rf2.getId(), projectId)).thenReturn(Optional.of(rf2));
        when(statsRepository.findByProjectId(projectId)).thenReturn(Map.of());

        ElementResponse response = elementService.update(projectId, rf2.getId(), userId,
                new UpdateElementRequest("  Nova descrição ", Priority.OPTIONAL));

        assertThat(response.code()).isEqualTo("RF2");
        assertThat(response.description()).isEqualTo("Nova descrição");
        assertThat(response.priority()).isEqualTo(Priority.OPTIONAL);
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.DRAFT);
        verify(projectRepository).touch(projectId);
    }

    @Test
    void edicaoRespeitaRegraDePrioridadeDoTipo() {
        loggedAs(MemberRole.EDITOR);
        Element rn1 = element(ElementKind.BUSINESS_RULE, "RN1");
        when(elementRepository.findByIdAndProjectId(rn1.getId(), projectId)).thenReturn(Optional.of(rn1));

        assertThatThrownBy(() -> elementService.update(projectId, rn1.getId(), userId,
                new UpdateElementRequest("x", Priority.MANDATORY)))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void visualizadorNaoEdita() {
        loggedAs(MemberRole.VIEWER);

        assertThatThrownBy(() -> elementService.update(projectId, UUID.randomUUID(), userId,
                new UpdateElementRequest("x", Priority.MANDATORY)))
                .isInstanceOf(ProjectAccessDeniedException.class);
    }
}
