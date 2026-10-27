package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.CreateVersionRequest;
import com.vvmonitor.api.dto.response.ModelChangesResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.Relationship;
import com.vvmonitor.domain.entity.RelationshipType;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.NothingToSubmitException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.VersionNotFoundException;
import com.vvmonitor.domain.version.ChangeSummary;
import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.ProjectVersionRepository;
import com.vvmonitor.infra.repository.ProjectVersionRepository.Latest;
import com.vvmonitor.infra.repository.ProjectVersionRepository.VersionRow;
import com.vvmonitor.infra.repository.RelationshipRepository;
import com.vvmonitor.infra.repository.RelationshipTypeRepository;
import com.vvmonitor.infra.repository.TestCaseRepository;
import com.vvmonitor.infra.repository.TestCoverageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VersionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T20:00:00Z");

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private ElementRepository elementRepository;
    private RelationshipRepository relationshipRepository;
    private ProjectVersionRepository versionRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private VersionService service;

    private Element rf1;
    private Element rf2;
    private RelationshipType dependencia;
    private UUID dependenciaId;

    @BeforeEach
    void setUp() {
        elementRepository = mock(ElementRepository.class);
        relationshipRepository = mock(RelationshipRepository.class);
        RelationshipTypeRepository typeRepository = mock(RelationshipTypeRepository.class);
        versionRepository = mock(ProjectVersionRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        service = new VersionService(elementRepository, relationshipRepository, typeRepository,
                mock(TestCaseRepository.class), mock(TestCoverageRepository.class), versionRepository,
                projectRepository, new ProjectAccessService(memberRepository), Clock.fixed(NOW, ZoneOffset.UTC));

        rf1 = element("RF1", "Cadastro");
        rf2 = element("RF2", "Login");
        dependencia = mock(RelationshipType.class);
        dependenciaId = UUID.randomUUID();
        when(dependencia.getId()).thenReturn(dependenciaId);
        when(dependencia.getName()).thenReturn("Dependência");
        when(typeRepository.findAvailableFor(projectId)).thenReturn(List.of(dependencia));
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rf1, rf2));
        when(relationshipRepository.findAllByProjectIdOrderByCreatedAt(projectId)).thenReturn(List.of());
        when(versionRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(versionRepository.find(eq(projectId), anyInt())).thenAnswer(inv -> Optional.of(new VersionRow(
                UUID.randomUUID(), inv.getArgument(1), null, userId, "Ana", NOW, 2, 0, 0, new ChangeSummary(2, 0, 0))));
        loggedAs(MemberRole.EDITOR);
    }

    private Element element(String code, String description) {
        Element e = new Element(projectId, ElementKind.FUNCTIONAL, code, description, Priority.MANDATORY, userId);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        return e;
    }

    private void loggedAs(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, role)));
    }

    /** Ultima versao gravada com o modelo como esta agora. */
    private void latestVersionIsCurrentModel(int number) {
        ModelSnapshot now = ModelSnapshot.of(List.of(rf1, rf2), List.of(), Map.of(dependencia.getId(), dependencia));
        when(versionRepository.findLatest(projectId)).thenReturn(Optional.of(new Latest(number, now)));
    }

    @Test
    void primeiraSubmissaoGeraV1ComTudoAdicionado() {
        service.create(projectId, userId, new CreateVersionRequest("  Revisão com o cliente "));

        ArgumentCaptor<ModelSnapshot> saved = ArgumentCaptor.forClass(ModelSnapshot.class);
        var order = inOrder(versionRepository);
        // trava o projeto antes de ler a ultima versao
        order.verify(versionRepository).lockProject(projectId);
        order.verify(versionRepository).findLatest(projectId);
        order.verify(versionRepository).insert(eq(projectId), eq(1), eq("Revisão com o cliente"), saved.capture(),
                eq(userId), eq(NOW));
        assertThat(saved.getValue().elements()).extracting(ModelSnapshot.ElementSnapshot::code)
                .containsExactly("RF1", "RF2");
        assertThat(saved.getValue().changes()).isEqualTo(new ChangeSummary(2, 0, 0));
        verify(projectRepository).touch(projectId);
    }

    @Test
    void proximaVersaoIncrementaONumeroENomeVazioViraNulo() {
        latestVersionIsCurrentModel(2);
        rf2.update("Login com e-mail", Priority.MANDATORY);

        service.create(projectId, userId, new CreateVersionRequest("   "));

        ArgumentCaptor<ModelSnapshot> saved = ArgumentCaptor.forClass(ModelSnapshot.class);
        verify(versionRepository).insert(eq(projectId), eq(3), eq(null), saved.capture(), eq(userId), eq(NOW));
        assertThat(saved.getValue().changes()).isEqualTo(new ChangeSummary(0, 1, 0));
    }

    @Test
    void semMudancasDesdeAUltimaVersaoRetornaConflito() {
        latestVersionIsCurrentModel(2);

        assertThatThrownBy(() -> service.create(projectId, userId, new CreateVersionRequest(null)))
                .isInstanceOf(NothingToSubmitException.class);
        verify(versionRepository, never()).insert(any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void visualizadorNaoSubmete() {
        loggedAs(MemberRole.VIEWER);

        assertThatThrownBy(() -> service.create(projectId, userId, new CreateVersionRequest(null)))
                .isInstanceOf(ProjectAccessDeniedException.class);
        verify(versionRepository, never()).lockProject(any());
    }

    @Test
    void mudancasIncluemRemocoesComRelacoesResolvidasPelaVersaoAnterior() {
        Relationship rf2DependeRf1 = new Relationship(projectId, rf2.getId(), rf1.getId(), dependencia.getId(), userId);
        ReflectionTestUtils.setField(rf2DependeRf1, "id", UUID.randomUUID());
        ModelSnapshot v1 = ModelSnapshot.of(List.of(rf1, rf2), List.of(rf2DependeRf1),
                Map.of(dependencia.getId(), dependencia));
        when(versionRepository.findLatest(projectId)).thenReturn(Optional.of(new Latest(1, v1)));
        // RF2 foi excluido depois da v1, levando a relacao junto
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rf1));

        ModelChangesResponse changes = service.changes(projectId, userId);

        assertThat(changes.baseVersion()).isEqualTo(1);
        assertThat(changes.summary()).isEqualTo(new ChangeSummary(0, 0, 2));
        assertThat(changes.elements().removed()).extracting(ModelSnapshot.ElementSnapshot::code).containsExactly("RF2");
        assertThat(changes.relationships().removed()).singleElement().satisfies(r -> {
            assertThat(r.source().code()).isEqualTo("RF2");
            assertThat(r.target().code()).isEqualTo("RF1");
            assertThat(r.type().name()).isEqualTo("Dependência");
        });
    }

    @Test
    void pendentesSaoOsAdicionadosOuAlteradosDesdeAUltimaVersao() {
        latestVersionIsCurrentModel(1);
        Element rf3 = element("RF3", "Novo");
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rf1, rf2, rf3));
        rf1.update("Cadastro com confirmação", Priority.MANDATORY);

        assertThat(service.pendingIds(projectId)).containsExactlyInAnyOrder(rf1.getId(), rf3.getId());
    }

    @Test
    void versaoInexistenteRetorna404() {
        loggedAs(MemberRole.VIEWER);
        when(versionRepository.find(projectId, 9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(projectId, 9, userId)).isInstanceOf(VersionNotFoundException.class);
    }

    private ModelSnapshot snapshotOf(Element... elements) {
        // sem chamar o mock aqui: o auxiliar e usado dentro de when(...)
        return ModelSnapshot.of(List.of(elements), List.of(), Map.of(dependenciaId, dependencia));
    }

    @Test
    void comparaComAAnteriorPorPadrao() {
        when(versionRepository.findSnapshot(projectId, 1)).thenReturn(Optional.of(snapshotOf(rf1)));
        when(versionRepository.findSnapshot(projectId, 2)).thenReturn(Optional.of(snapshotOf(rf1, rf2)));

        ModelChangesResponse changes = service.compare(projectId, 2, null, userId);

        assertThat(changes.baseVersion()).isEqualTo(1);
        assertThat(changes.targetVersion()).isEqualTo(2);
        assertThat(changes.elements().added()).extracting(ModelSnapshot.ElementSnapshot::code).containsExactly("RF2");
    }

    @Test
    void primeiraVersaoComparadaComAAnteriorTrazTudoComoAdicionado() {
        when(versionRepository.findSnapshot(projectId, 1)).thenReturn(Optional.of(snapshotOf(rf1, rf2)));

        ModelChangesResponse changes = service.compare(projectId, 1, "previous", userId);

        assertThat(changes.baseVersion()).isNull();
        assertThat(changes.summary()).isEqualTo(new ChangeSummary(2, 0, 0));
    }

    @Test
    void comparaComOModeloAtual() {
        when(versionRepository.findSnapshot(projectId, 1)).thenReturn(Optional.of(snapshotOf(rf1)));

        ModelChangesResponse changes = service.compare(projectId, 1, "current", userId);

        assertThat(changes.baseVersion()).isEqualTo(1);
        assertThat(changes.targetVersion()).isNull();
        assertThat(changes.elements().added()).extracting(ModelSnapshot.ElementSnapshot::code).containsExactly("RF2");
    }

    @Test
    void comparaDuasVersoesSempreDaMaisAntigaParaAMaisNova() {
        when(versionRepository.findSnapshot(projectId, 1)).thenReturn(Optional.of(snapshotOf(rf1, rf2)));
        when(versionRepository.findSnapshot(projectId, 3)).thenReturn(Optional.of(snapshotOf(rf1)));

        ModelChangesResponse changes = service.compare(projectId, 3, "1", userId);

        assertThat(changes.baseVersion()).isEqualTo(1);
        assertThat(changes.targetVersion()).isEqualTo(3);
        assertThat(changes.elements().removed()).extracting(ModelSnapshot.ElementSnapshot::code).containsExactly("RF2");
    }

    @Test
    void comparacaoInvalidaRetorna400EVersaoInexistente404() {
        when(versionRepository.findSnapshot(projectId, 1)).thenReturn(Optional.of(snapshotOf(rf1)));

        assertThatThrownBy(() -> service.compare(projectId, 1, "ontem", userId))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> service.compare(projectId, 1, "7", userId))
                .isInstanceOf(VersionNotFoundException.class);
    }
}
