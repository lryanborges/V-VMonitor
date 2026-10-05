package com.vvmonitor.service;

import com.vvmonitor.api.dto.response.DeleteImpactResponse;
import com.vvmonitor.api.dto.response.DeleteImpactResponse.ImpactLevel;
import com.vvmonitor.api.dto.response.ElementSummary;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.exception.ElementNotFoundException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository;
import com.vvmonitor.infra.repository.ElementStatsRepository.ElementStats;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.RelationshipTypeRepository;
import com.vvmonitor.infra.repository.TestCoverageQueryRepository;
import com.vvmonitor.infra.repository.TestCoverageQueryRepository.TestSummaryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElementRemovalServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private ElementRepository elementRepository;
    private RelationshipRepository relationshipRepository;
    private TestCoverageQueryRepository coverageRepository;
    private ElementStatsRepository statsRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private ElementRemovalService service;

    private final List<Element> elements = new ArrayList<>();
    private final List<Relationship> relationships = new ArrayList<>();
    private RelationshipType dependencia;
    private RelationshipType refinamento;

    @BeforeEach
    void setUp() {
        elementRepository = mock(ElementRepository.class);
        relationshipRepository = mock(RelationshipRepository.class);
        RelationshipTypeRepository typeRepository = mock(RelationshipTypeRepository.class);
        coverageRepository = mock(TestCoverageQueryRepository.class);
        statsRepository = mock(ElementStatsRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        service = new ElementRemovalService(elementRepository, relationshipRepository, typeRepository,
                coverageRepository, statsRepository, projectRepository, new ProjectAccessService(memberRepository));

        dependencia = type("Dependência");
        refinamento = type("Refinamento");
        when(typeRepository.findAvailableFor(projectId)).thenReturn(List.of(dependencia, refinamento));
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(elements);
        when(relationshipRepository.findAllByProjectIdOrderByCreatedAt(projectId)).thenReturn(relationships);
        when(elementRepository.findByIdAndProjectId(any(), any())).thenReturn(Optional.empty());
        when(coverageRepository.testsOnlyCovering(any())).thenReturn(List.of());
        when(statsRepository.findByProjectId(projectId)).thenReturn(Map.of());
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, MemberRole.EDITOR)));
    }

    private RelationshipType type(String name) {
        RelationshipType t = RelationshipType.custom(null, name);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        return t;
    }

    private Element element(String code) {
        Element e = new Element(projectId, ElementKind.FUNCTIONAL, code, "desc", Priority.MANDATORY, userId);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        elements.add(e);
        when(elementRepository.findByIdAndProjectId(e.getId(), projectId)).thenReturn(Optional.of(e));
        return e;
    }

    private void relate(Element source, RelationshipType type, Element target) {
        Relationship r = new Relationship(projectId, source.getId(), target.getId(), type.getId(), userId);
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        relationships.add(r);
    }

    @Test
    void elementoIsoladoTemImpactoBaixo() {
        Element rf9 = element("RF9");

        DeleteImpactResponse impact = service.impact(projectId, rf9.getId(), userId);

        assertThat(impact.level()).isEqualTo(ImpactLevel.LOW);
        assertThat(impact.relationships()).isEmpty();
    }

    @Test
    void comRelacoesSemOrfaosEhMedio() {
        Element rf7 = element("RF7");
        Element rf5 = element("RF5");
        Element rf6 = element("RF6");
        relate(rf7, dependencia, rf5);
        relate(rf5, refinamento, rf6); // RF5 continua ligado ao RF6 depois da exclusao

        DeleteImpactResponse impact = service.impact(projectId, rf7.getId(), userId);

        assertThat(impact.level()).isEqualTo(ImpactLevel.MEDIUM);
        assertThat(impact.relationships()).singleElement()
                .satisfies(r -> {
                    assertThat(r.typeName()).isEqualTo("Dependência");
                    assertThat(r.outgoing()).isTrue();
                    assertThat(r.other().code()).isEqualTo("RF5");
                });
        assertThat(impact.orphans()).isEmpty();
    }

    @Test
    void vizinhoQueSoSeLigaAoExcluidoFicaOrfaoEImpactoAlto() {
        Element rf7 = element("RF7");
        Element rf8 = element("RF8");
        relate(rf8, refinamento, rf7);

        DeleteImpactResponse impact = service.impact(projectId, rf7.getId(), userId);

        assertThat(impact.level()).isEqualTo(ImpactLevel.HIGH);
        assertThat(impact.orphans()).extracting(ElementSummary::code).containsExactly("RF8");
    }

    @Test
    void quemDependeDoExcluidoApareceComoDependente() {
        Element rf7 = element("RF7");
        Element rf10 = element("RF10");
        Element rf11 = element("RF11");
        Element rf1 = element("RF1");
        relate(rf10, dependencia, rf7);
        relate(rf11, dependencia, rf7);
        relate(rf10, refinamento, rf1);
        relate(rf11, refinamento, rf1);

        DeleteImpactResponse impact = service.impact(projectId, rf7.getId(), userId);

        assertThat(impact.dependents()).extracting(ElementSummary::code).containsExactly("RF10", "RF11");
        assertThat(impact.orphans()).isEmpty();
    }

    @Test
    void testeQueSoCobreOExcluidoDeixaImpactoAlto() {
        Element rf7 = element("RF7");
        when(coverageRepository.testsOnlyCovering(rf7.getId()))
                .thenReturn(List.of(new TestSummaryView(UUID.randomUUID(), "T7", "teste")));
        when(statsRepository.findByProjectId(projectId)).thenReturn(Map.of(rf7.getId(), new ElementStats(0, 1)));

        DeleteImpactResponse impact = service.impact(projectId, rf7.getId(), userId);

        assertThat(impact.level()).isEqualTo(ImpactLevel.HIGH);
        assertThat(impact.testsLeftWithoutRequirement()).extracting(DeleteImpactResponse.TestSummary::code)
                .containsExactly("T7");
        assertThat(impact.tests()).isEqualTo(1);
    }

    @Test
    void regraDoNivel() {
        assertThat(ElementRemovalService.impactLevel(0, 0, 0, 0)).isEqualTo(ImpactLevel.LOW);
        assertThat(ElementRemovalService.impactLevel(0, 2, 0, 0)).isEqualTo(ImpactLevel.MEDIUM);
        assertThat(ElementRemovalService.impactLevel(4, 0, 0, 0)).isEqualTo(ImpactLevel.MEDIUM);
        assertThat(ElementRemovalService.impactLevel(5, 0, 0, 0)).isEqualTo(ImpactLevel.HIGH);
        assertThat(ElementRemovalService.impactLevel(1, 0, 1, 0)).isEqualTo(ImpactLevel.HIGH);
        assertThat(ElementRemovalService.impactLevel(0, 1, 0, 1)).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void exclusaoRemoveAssociacoesAntesDoElemento() {
        Element rf7 = element("RF7");

        service.delete(projectId, rf7.getId(), userId);

        InOrder order = inOrder(relationshipRepository, coverageRepository, elementRepository, projectRepository);
        order.verify(relationshipRepository).softDeleteAllOfElement(rf7.getId());
        order.verify(coverageRepository).softDeleteCoverageOf(rf7.getId());
        order.verify(elementRepository).delete(rf7);
        order.verify(projectRepository).touch(projectId);
    }

    @Test
    void elementoInexistenteNaoEhExcluido() {
        assertThatThrownBy(() -> service.delete(projectId, UUID.randomUUID(), userId))
                .isInstanceOf(ElementNotFoundException.class);
        verify(elementRepository, never()).delete(any());
    }

    @Test
    void visualizadorNaoExcluiNemVeImpacto() {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, MemberRole.VIEWER)));
        Element rf7 = element("RF7");

        assertThatThrownBy(() -> service.delete(projectId, rf7.getId(), userId))
                .isInstanceOf(ProjectAccessDeniedException.class);
        assertThatThrownBy(() -> service.impact(projectId, rf7.getId(), userId))
                .isInstanceOf(ProjectAccessDeniedException.class);
    }
}
