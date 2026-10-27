package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.TestRequest;
import com.vvmonitor.api.dto.response.TestResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.TestService;
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

/** Testes e a cobertura dos requisitos (RF9, RF13). */
@RestController
@RequestMapping("/api/projects/{projectId}/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    public List<TestResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID projectId) {
        return testService.list(projectId, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable UUID projectId,
                               @Valid @RequestBody TestRequest request) {
        return testService.create(projectId, user.id(), request);
    }

    @PutMapping("/{testId}")
    public TestResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable UUID projectId,
                               @PathVariable UUID testId,
                               @Valid @RequestBody TestRequest request) {
        return testService.update(projectId, testId, user.id(), request);
    }

    @DeleteMapping("/{testId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable UUID projectId,
                       @PathVariable UUID testId) {
        testService.delete(projectId, testId, user.id());
    }
}
