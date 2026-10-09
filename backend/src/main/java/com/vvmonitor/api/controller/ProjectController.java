package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.CreateProjectRequest;
import com.vvmonitor.api.dto.response.ProjectResponse;
import com.vvmonitor.api.dto.response.SubmissionResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.ProjectService;
import com.vvmonitor.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final SubmissionService submissionService;

    public ProjectController(ProjectService projectService, SubmissionService submissionService) {
        this.projectService = projectService;
        this.submissionService = submissionService;
    }

    /** RF3, UC-04: projetos proprios e compartilhados; filtros e busca ficam no frontend. */
    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return projectService.listAccessible(user.id());
    }

    /** RF4, UC-03: cria o projeto; o criador vira OWNER. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(user.id(), request);
    }

    /** UC-04, passo 4: dados do projeto selecionado. */
    @GetMapping("/{id}")
    public ProjectResponse findById(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return projectService.findById(id, user.id());
    }

    /** RF10: submete todos os elementos e relacoes em rascunho do projeto. */
    @PostMapping("/{id}/submit")
    public SubmissionResponse submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return submissionService.submit(id, user.id());
    }
}
