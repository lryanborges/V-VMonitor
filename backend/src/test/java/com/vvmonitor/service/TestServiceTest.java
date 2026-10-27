package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.TestRequest;
import com.vvmonitor.api.dto.response.ElementSummary;
import com.vvmonitor.api.dto.response.TestResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.TestCase;
import com.vvmonitor.domain.entity.TestCoverage;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.enums.SequenceKind;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.TestNotFoundException;
import com.vvmonitor.infra.repository.CodeSequenceRepository;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.GraphPositionRepository;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.TestCaseRepository;
import com.vvmonitor.infra.repository.TestCoverageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private TestCaseRepository testRepository;
    private TestCoverageRepository coverageRepository;
    private ElementRepository elementRepository;
    private CodeSequenceRepository codeSequenceRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private VersionService versionService;
    private GraphPositionRepository positionRepository;
    private TestService service;

    private Element rf1;
    private Element rf2;
    private Element rnf1;
    private Element rn1;

    @BeforeEach
    void setUp() {
        testRepository = mock(TestCaseRepository.class);
        coverageRepository = mock(TestCoverageRepository.class);
        elementRepository = mock(ElementRepository.class);
        codeSequenceRepository = mock(CodeSequenceRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        versionService = mock(VersionService.class);
        positionRepository = mock(GraphPositionRepository.class);
        service = new TestService(testRepository, coverageRepository, elementRepository, codeSequenceRepository,
                projectRepository, new ProjectAccessService(memberRepository), versionService, positionRepository);

        rf1 = element(ElementKind.FUNCTIONAL, "RF1");
        rf2 = element(ElementKind.FUNCTIONAL, "RF2");
        rnf1 = element(ElementKind.NON_FUNCTIONAL, "RNF1");
        rn1 = element(ElementKind.BUSINESS_RULE, "RN1");
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rf1, rf2, rnf1, rn1));
        when(testRepository.saveAndFlush(any(TestCase.class))).thenAnswer(inv -> {
            TestCase t = inv.getArgument(0);
            if (t.getId() == null) {
                ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            }
            return t;
        });
        when(memberRepository.findByProjectIdAndUserId(any(), any())).thenReturn(Optional.empty());
        loggedAs(MemberRole.EDITOR);
    }

    private Element element(ElementKind kind, String code) {
        Element e = new Element(projectId, kind, code, "d " + code, kind.isRequirement() ? Priority.MANDATORY : null,
                userId);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        return e;
    }

    private void loggedAs(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, role)));
    }

    private TestCase existing(int number, Element... covers) {
        TestCase t = new TestCase(projectId, number, "Teste " + number, userId);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        when(testRepository.findByIdAndProjectId(t.getId(), projectId)).thenReturn(Optional.of(t));
        List<TestCoverage> coverage = java.util.Arrays.stream(covers).map(e -> new TestCoverage(t.getId(), e.getId())).toList();
        when(coverageRepository.findAllByTestId(t.getId())).thenReturn(coverage);
        return t;
    }

    @Test
    void criaComCodigoTECobreOsRequisitos() {
        when(codeSequenceRepository.allocate(projectId, SequenceKind.TEST)).thenReturn(3);

        TestResponse response = service.create(projectId, userId,
                new TestRequest("  Login com senha errada ", List.of(rnf1.getId(), rf2.getId(), rf2.getId())));

        assertThat(response.code()).isEqualTo("T3");
        assertThat(response.description()).isEqualTo("Login com senha errada");
        // sem repeticao e em ordem (RF antes de RNF)
        assertThat(response.covers()).extracting(ElementSummary::code).containsExactly("RF2", "RNF1");
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.DRAFT);
        verify(coverageRepository, times(2)).save(any(TestCoverage.class));
        verify(projectRepository).touch(projectId);
    }

    @Test
    void regraDeNegocioNaoECobertaPorTeste() {
        assertThatThrownBy(() -> service.create(projectId, userId, new TestRequest("x", List.of(rf1.getId(), rn1.getId()))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("RN1");
        verify(testRepository, never()).saveAndFlush(any());
    }

    @Test
    void requisitoDeOutroProjetoERecusado() {
        assertThatThrownBy(() -> service.create(projectId, userId, new TestRequest("x", List.of(UUID.randomUUID()))))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void edicaoTrocaDescricaoERequisitosCobertos() {
        TestCase t1 = existing(1, rf1, rf2);
        List<TestCoverage> before = coverageRepository.findAllByTestId(t1.getId());

        TestResponse response = service.update(projectId, t1.getId(), userId,
                new TestRequest("Novo texto", List.of(rf2.getId(), rnf1.getId())));

        assertThat(response.description()).isEqualTo("Novo texto");
        assertThat(response.covers()).extracting(ElementSummary::code).containsExactly("RF2", "RNF1");
        // RF1 saiu (vinculo removido), RF2 ficou (intocado), RNF1 entrou (novo vinculo)
        verify(coverageRepository).delete(before.get(0));
        verify(coverageRepository, never()).delete(before.get(1));
        ArgumentCaptor<TestCoverage> saved = ArgumentCaptor.forClass(TestCoverage.class);
        verify(coverageRepository).save(saved.capture());
        assertThat(saved.getValue().getElementId()).isEqualTo(rnf1.getId());
    }

    @Test
    void exclusaoRemoveOTesteESeusVinculos() {
        TestCase t1 = existing(1, rf1);

        service.delete(projectId, t1.getId(), userId);

        verify(coverageRepository).delete(any(TestCoverage.class));
        verify(testRepository).delete(t1);
        verify(positionRepository).deleteNodes(List.of(t1.getId()));
    }

    @Test
    void listaComRequisitosCobertosEPendencia() {
        TestCase t2 = existing(2, rf1);
        TestCase t10 = existing(10);
        when(testRepository.findAllByProjectId(projectId)).thenReturn(List.of(t10, t2));
        when(coverageRepository.findAllOfProject(projectId)).thenReturn(List.of(new TestCoverage(t2.getId(), rf1.getId())));
        when(versionService.pendingIds(projectId)).thenReturn(Set.of(t10.getId()));

        List<TestResponse> tests = service.list(projectId, userId);

        assertThat(tests).extracting(TestResponse::code).containsExactly("T2", "T10");
        assertThat(tests.get(0).covers()).extracting(ElementSummary::code).containsExactly("RF1");
        assertThat(tests.get(0).submissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        // teste que ficou sem requisito (o requisito foi excluido)
        assertThat(tests.get(1).covers()).isEmpty();
        assertThat(tests.get(1).submissionStatus()).isEqualTo(SubmissionStatus.DRAFT);
    }

    @Test
    void visualizadorNaoAlteraETesteInexistenteRetorna404() {
        loggedAs(MemberRole.VIEWER);
        assertThatThrownBy(() -> service.create(projectId, userId, new TestRequest("x", List.of(rf1.getId()))))
                .isInstanceOf(ProjectAccessDeniedException.class);

        loggedAs(MemberRole.EDITOR);
        assertThatThrownBy(() -> service.delete(projectId, UUID.randomUUID(), userId))
                .isInstanceOf(TestNotFoundException.class);
    }
}
