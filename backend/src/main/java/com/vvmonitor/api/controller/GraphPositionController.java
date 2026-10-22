package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.SaveGraphPositionsRequest;
import com.vvmonitor.api.dto.response.GraphPositionResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.GraphPositionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Posicao dos nos na visualizacao em grafo (RF21). */
@RestController
@RequestMapping("/api/projects/{projectId}/graph-positions")
public class GraphPositionController {

    private final GraphPositionService positionService;

    public GraphPositionController(GraphPositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public List<GraphPositionResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID projectId) {
        return positionService.list(projectId, user.id());
    }

    /** Salva as posicoes de nos arrastados (proprietario e editores). */
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@AuthenticationPrincipal AuthenticatedUser user,
                     @PathVariable UUID projectId,
                     @Valid @RequestBody SaveGraphPositionsRequest request) {
        positionService.save(projectId, user.id(), request);
    }
}
