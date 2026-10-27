package com.vvmonitor.service;

import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.VersionNotFoundException;
import com.vvmonitor.domain.version.ModelSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.ElementSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.RelationshipSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TestSnapshot;
import com.vvmonitor.domain.version.ModelSnapshot.TypeSnapshot;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.ProjectRepository;
import com.vvmonitor.infra.repository.ProjectVersionRepository;
import com.vvmonitor.infra.repository.RestoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestoreServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private RestoreRepository restoreRepository;
    private ProjectVersionRepository versionRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository memberRepository;
    private RestoreService service;

    private final ElementSnapshot rf1 = new ElementSnapshot(UUID.randomUUID(), "RF1", ElementKind.FUNCTIONAL, "Cadastro", Priority.MANDATORY);
    private final ElementSnapshot rf2 = new ElementSnapshot(UUID.randomUUID(), "RF2", ElementKind.FUNCTIONAL, "Login", Priority.DESIRABLE);
    private final UUID valida = UUID.randomUUID();
    private final RelationshipSnapshot rel = new RelationshipSnapshot(UUID.randomUUID(), rf2.id(), rf1.id(), valida);
    private final TestSnapshot t1 = new TestSnapshot(UUID.randomUUID(), "T1", "Login ok", List.of(rf2.id()));

    @BeforeEach
    void setUp() {
        restoreRepository = mock(RestoreRepository.class);
        versionRepository = mock(ProjectVersionRepository.class);
        projectRepository = mock(ProjectRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        service = new RestoreService(restoreRepository, versionRepository, projectRepository,
                new ProjectAccessService(memberRepository));
        ModelSnapshot v2 = new ModelSnapshot(ModelSnapshot.FORMAT, List.of(rf1, rf2), List.of(rel),
                List.of(new TypeSnapshot(valida, "Valida", false, true)), List.of(t1), null);
        when(versionRepository.findSnapshot(projectId, 2)).thenReturn(Optional.of(v2));
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, MemberRole.EDITOR)));
    }

    @Test
    void removeOQueFoiCriadoDepoisEVoltaOQueExistia() {
        UUID rf3 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        when(restoreRepository.activeIds("elements", projectId)).thenReturn(List.of(rf1.id(), rf3));
        when(restoreRepository.activeIds("tests", projectId)).thenReturn(List.of(t2));
        when(restoreRepository.activeIds("relationships", projectId)).thenReturn(List.of());

        service.restore(projectId, 2, userId);

        InOrder order = inOrder(versionRepository, restoreRepository, projectRepository);
        // trava antes de ler a versao
        order.verify(versionRepository).lockProject(projectId);
        // primeiro sai o que nao existia na versao (RF3 e T2, criados depois)...
        order.verify(restoreRepository).softDelete("elements", projectId, List.of(rf3));
        order.verify(restoreRepository).softDelete("tests", projectId, List.of(t2));
        order.verify(restoreRepository).softDelete("relationships", projectId, List.of());
        // ...depois volta o que existia, como estava (o RF2 excluido volta com o mesmo id e codigo)
        order.verify(restoreRepository).restoreTypes(projectId, List.of(valida));
        order.verify(restoreRepository).restoreElement(projectId, rf1.id(), "Cadastro", Priority.MANDATORY);
        order.verify(restoreRepository).restoreElement(projectId, rf2.id(), "Login", Priority.DESIRABLE);
        order.verify(restoreRepository).restoreRelationship(projectId, rel.id(), rf2.id(), rf1.id(), valida);
        order.verify(restoreRepository).restoreTest(projectId, t1.id(), "Login ok");
        order.verify(restoreRepository).restoreCoverage(t1.id(), List.of(rf2.id()));
        order.verify(projectRepository).touch(projectId);
        // nenhuma versao e criada
        verify(versionRepository, never()).insert(any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void visualizadorNaoRestauraEVersaoInexistenteRetorna404() {
        assertThatThrownBy(() -> service.restore(projectId, 9, userId)).isInstanceOf(VersionNotFoundException.class);

        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, MemberRole.VIEWER)));
        assertThatThrownBy(() -> service.restore(projectId, 2, userId)).isInstanceOf(ProjectAccessDeniedException.class);
        verify(restoreRepository, never()).restoreElement(eq(projectId), any(), any(), any());
    }
}
