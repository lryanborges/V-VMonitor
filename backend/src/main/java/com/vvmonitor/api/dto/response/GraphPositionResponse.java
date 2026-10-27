package com.vvmonitor.api.dto.response;

import com.vvmonitor.infra.repository.GraphPositionRepository.Position;

import java.util.UUID;

/** RF21: posicao salva de um no do grafo (canto superior esquerdo, nas coordenadas do React Flow). */
public record GraphPositionResponse(UUID nodeId, double x, double y) {

    public static GraphPositionResponse from(Position position) {
        return new GraphPositionResponse(position.nodeId(), position.x(), position.y());
    }
}
