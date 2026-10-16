package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.CreateVersionRequest;
import com.vvmonitor.api.dto.response.ModelChangesResponse;
import com.vvmonitor.api.dto.response.VersionResponse;
import com.vvmonitor.api.dto.response.VersionSummaryResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Submissao do modelo como versao (RF10 + RF17) e consulta das versoes (RF18). */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    /** O que mudou desde a ultima versao (adicionados, alterados e removidos). */
    @GetMapping("/changes")
    public ModelChangesResponse changes(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable UUID projectId) {
        return versionService.changes(projectId, user.id());
    }

    /** Submete o modelo: grava a proxima versao, com nome opcional. */
    @PostMapping("/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionSummaryResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable UUID projectId,
                                         @Valid @RequestBody CreateVersionRequest request) {
        return versionService.create(projectId, user.id(), request);
    }

    @GetMapping("/versions")
    public List<VersionSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable UUID projectId) {
        return versionService.list(projectId, user.id());
    }

    /** Uma versao com o modelo completo daquele momento (so leitura). */
    @GetMapping("/versions/{number}")
    public VersionResponse find(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable UUID projectId,
                                @PathVariable int number) {
        return versionService.find(projectId, number, user.id());
    }

    /**
     * RF18: compara a versao com outra. with = previous (padrao), current ou o numero de outra versao.
     */
    @GetMapping("/versions/{number}/compare")
    public ModelChangesResponse compare(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable UUID projectId,
                                        @PathVariable int number,
                                        @RequestParam(defaultValue = "previous") String with) {
        return versionService.compare(projectId, number, with, user.id());
    }
}
