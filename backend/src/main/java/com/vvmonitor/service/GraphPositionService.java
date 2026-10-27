package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.SaveGraphPositionsRequest;
import com.vvmonitor.api.dto.response.GraphPositionResponse;
import com.vvmonitor.domain.entity.Element;
import com.vvmonitor.domain.entity.TestCase;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.infra.repository.ElementRepository;
import com.vvmonitor.infra.repository.GraphPositionRepository;
import com.vvmonitor.infra.repository.GraphPositionRepository.Position;
import com.vvmonitor.infra.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Posicao dos nos no grafo (RF21, proposto para a v2 do documento): compartilhada entre os membros, salva so por
 * proprietario e editores. E apresentacao, nao conteudo do modelo: nao entra nas versoes nem nas pendencias.
 */
@Service
public class GraphPositionService {

    private final GraphPositionRepository positionRepository;
    private final ElementRepository elementRepository;
    private final TestCaseRepository testRepository;
    private final ProjectAccessService accessService;

    public GraphPositionService(GraphPositionRepository positionRepository, ElementRepository elementRepository,
                                TestCaseRepository testRepository, ProjectAccessService accessService) {
        this.positionRepository = positionRepository;
        this.elementRepository = elementRepository;
        this.testRepository = testRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<GraphPositionResponse> list(UUID projectId, UUID userId) {
        accessService.requireMember(projectId, userId);
        return positionRepository.findByProject(projectId).stream().map(GraphPositionResponse::from).toList();
    }

    /** Grava (ou atualiza) a posicao dos nos informados; todos precisam ser elementos ou testes do projeto. */
    @Transactional
    public void save(UUID projectId, UUID userId, SaveGraphPositionsRequest request) {
        accessService.requireRole(projectId, userId, MemberRole.EDITOR);
        Set<UUID> nodes = new HashSet<>();
        elementRepository.findAllByProjectId(projectId).stream().map(Element::getId).forEach(nodes::add);
        testRepository.findAllByProjectId(projectId).stream().map(TestCase::getId).forEach(nodes::add);
        List<Position> positions = request.positions().stream().map(p -> {
            if (!nodes.contains(p.nodeId())) {
                throw new InvalidFieldException("positions", "Elemento ou teste não encontrado neste projeto.");
            }
            if (!Double.isFinite(p.x()) || !Double.isFinite(p.y())) {
                throw new InvalidFieldException("positions", "Posição inválida.");
            }
            return new Position(p.nodeId(), p.x(), p.y());
        }).toList();
        positionRepository.saveAll(projectId, userId, positions);
    }
}
