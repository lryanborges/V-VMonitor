package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.CreateRelationshipRequest;
import com.vvmonitor.api.dto.request.CreateRelationshipTypeRequest;
import com.vvmonitor.api.dto.request.UpdateRelationshipRequest;
import com.vvmonitor.api.dto.response.RelationshipResponse;
import com.vvmonitor.api.dto.response.RelationshipTypeResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.RelationshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Relacionamentos (RF7) e tipos de relacionamento (RF8) de um projeto, UC-06. */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @GetMapping("/relationship-types")
    public List<RelationshipTypeResponse> listTypes(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable UUID projectId) {
        return relationshipService.listTypes(projectId, user.id());
    }

    /** UC-06, sequencia alternativa: novo tipo de relacionamento. */
    @PostMapping("/relationship-types")
    @ResponseStatus(HttpStatus.CREATED)
    public RelationshipTypeResponse createType(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable UUID projectId,
                                               @Valid @RequestBody CreateRelationshipTypeRequest request) {
        return relationshipService.createType(projectId, user.id(), request);
    }

    @GetMapping("/relationships")
    public List<RelationshipResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID projectId) {
        return relationshipService.list(projectId, user.id());
    }

    /** UC-06, sequencia tipica: relaciona dois elementos com um tipo. */
    @PostMapping("/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public RelationshipResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID projectId,
                                       @Valid @RequestBody CreateRelationshipRequest request) {
        return relationshipService.create(projectId, user.id(), request);
    }

    /** RF14: troca o tipo e/ou inverte a direcao da relacao. */
    @PutMapping("/relationships/{relationshipId}")
    public RelationshipResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID projectId,
                                       @PathVariable UUID relationshipId,
                                       @Valid @RequestBody UpdateRelationshipRequest request) {
        return relationshipService.update(projectId, relationshipId, user.id(), request);
    }

    /** RF15: remove a relacao; os elementos continuam no modelo. */
    @DeleteMapping("/relationships/{relationshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable UUID projectId,
                       @PathVariable UUID relationshipId) {
        relationshipService.delete(projectId, relationshipId, user.id());
    }
}
