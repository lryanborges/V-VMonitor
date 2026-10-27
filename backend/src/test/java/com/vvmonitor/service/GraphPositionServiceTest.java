package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.SaveGraphPositionsRequest;
import com.vvmonitor.api.dto.request.SaveGraphPositionsRequest.NodePosition;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.ProjectMember;
import com.vvmonitor.domain.entity.TestCase;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.GraphPositionRepository;
import com.vvmonitor.infra.repository.GraphPositionRepository.Position;
import com.vvmonitor.infra.repository.ProjectMemberRepository;
import com.vvmonitor.infra.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class GraphPositionServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private GraphPositionRepository positionRepository;
    private ProjectMemberRepository memberRepository;
    private GraphPositionService service;
    private Element rf1;
    private TestCase t1;

    @BeforeEach
    void setUp() {
        positionRepository = mock(GraphPositionRepository.class);
        ElementRepository elementRepository = mock(ElementRepository.class);
        TestCaseRepository testRepository = mock(TestCaseRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        service = new GraphPositionService(positionRepository, elementRepository, testRepository,
                new ProjectAccessService(memberRepository));

        rf1 = new Element(projectId, ElementKind.FUNCTIONAL, "RF1", "Cadastro", Priority.MANDATORY, userId);
        ReflectionTestUtils.setField(rf1, "id", UUID.randomUUID());
        t1 = new TestCase(projectId, 1, "Cadastro valido", userId);
        ReflectionTestUtils.setField(t1, "id", UUID.randomUUID());
        when(elementRepository.findAllByProjectId(projectId)).thenReturn(List.of(rf1));
        when(testRepository.findAllByProjectId(projectId)).thenReturn(List.of(t1));
        loggedAs(MemberRole.EDITOR);
    }

    private void loggedAs(MemberRole role) {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(new ProjectMember(projectId, userId, role)));
    }

    @Test
    void editorSalvaPosicoesDeElementosETestes() {
        service.save(projectId, userId, new SaveGraphPositionsRequest(List.of(
                new NodePosition(rf1.getId(), 10.5, -20.0), new NodePosition(t1.getId(), 300.0, 40.0))));

        verify(positionRepository).saveAll(projectId, userId, List.of(
                new Position(rf1.getId(), 10.5, -20.0), new Position(t1.getId(), 300.0, 40.0)));
    }

    @Test
    void noDeOutroProjetoOuPosicaoInvalidaSaoRecusados() {
        assertThatThrownBy(() -> service.save(projectId, userId, new SaveGraphPositionsRequest(List.of(
                new NodePosition(UUID.randomUUID(), 0.0, 0.0)))))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> service.save(projectId, userId, new SaveGraphPositionsRequest(List.of(
                new NodePosition(rf1.getId(), Double.NaN, 0.0)))))
                .isInstanceOf(InvalidFieldException.class);
        verify(positionRepository, never()).saveAll(any(), any(), any());
    }

    @Test
    void visualizadorVeMasNaoSalva() {
        loggedAs(MemberRole.VIEWER);
        when(positionRepository.findByProject(projectId)).thenReturn(List.of(new Position(rf1.getId(), 1.0, 2.0)));

        assertThat(service.list(projectId, userId)).singleElement().satisfies(p -> assertThat(p.x()).isEqualTo(1.0));
        assertThatThrownBy(() -> service.save(projectId, userId, new SaveGraphPositionsRequest(List.of(
                new NodePosition(rf1.getId(), 0.0, 0.0)))))
                .isInstanceOf(ProjectAccessDeniedException.class);
    }
}
