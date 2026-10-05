package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.CreateElementRequest;
import com.vvmonitor.api.dto.request.UpdateElementRequest;
import com.vvmonitor.api.dto.response.DeleteImpactResponse;
import com.vvmonitor.api.dto.response.ElementResponse;
import com.vvmonitor.api.dto.response.NextCodeResponse;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.ElementRemovalService;
import com.vvmonitor.service.ElementService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Requisitos funcionais, nao funcionais e regras de negocio de um projeto (RF5, RF6). */
@RestController
@RequestMapping("/api/projects/{projectId}/elements")
public class ElementController {

    private final ElementService elementService;
    private final ElementRemovalService removalService;

    public ElementController(ElementService elementService, ElementRemovalService removalService) {
        this.elementService = elementService;
        this.removalService = removalService;
    }

    /** Lista completa; abas por tipo e busca ficam no frontend. */
    @GetMapping
    public List<ElementResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable UUID projectId) {
        return elementService.list(projectId, user.id());
    }

    /** UC-05 e UC-07: cadastra requisito ou regra de negocio; exige permissao de edicao. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ElementResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable UUID projectId,
                                  @Valid @RequestBody CreateElementRequest request) {
        return elementService.create(projectId, user.id(), request);
    }

    /** Previa do proximo codigo do tipo (ex.: RF21), exibida no formulario. */
    @GetMapping("/next-code")
    public NextCodeResponse nextCode(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable UUID projectId,
                                     @RequestParam ElementKind kind) {
        return elementService.nextCode(projectId, user.id(), kind);
    }

    @GetMapping("/{elementId}")
    public ElementResponse findById(@AuthenticationPrincipal AuthenticatedUser user,
                                    @PathVariable UUID projectId,
                                    @PathVariable UUID elementId) {
        return elementService.findById(projectId, elementId, user.id());
    }

    /** RF14: edita descricao e prioridade (tipo e codigo sao fixos). */
    @PutMapping("/{elementId}")
    public ElementResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable UUID projectId,
                                  @PathVariable UUID elementId,
                                  @Valid @RequestBody UpdateElementRequest request) {
        return elementService.update(projectId, elementId, user.id(), request);
    }

    /** RF16: o que muda no modelo se o elemento for excluido (nada e alterado). */
    @GetMapping("/{elementId}/delete-impact")
    public DeleteImpactResponse deleteImpact(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable UUID projectId,
                                             @PathVariable UUID elementId) {
        return removalService.impact(projectId, elementId, user.id());
    }

    /** RF15: exclui o elemento e suas associacoes. */
    @DeleteMapping("/{elementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable UUID projectId,
                       @PathVariable UUID elementId) {
        removalService.delete(projectId, elementId, user.id());
    }
}
