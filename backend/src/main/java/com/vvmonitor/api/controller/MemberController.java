package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.AddMemberRequest;
import com.vvmonitor.api.dto.response.MemberResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Pessoas com acesso ao projeto (RF20). */
@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<MemberResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID projectId) {
        return memberService.list(projectId, user.id());
    }

    /** So o proprietario; a pessoa entra como editor. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse add(@AuthenticationPrincipal AuthenticatedUser user,
                              @PathVariable UUID projectId,
                              @Valid @RequestBody AddMemberRequest request) {
        return memberService.add(projectId, user.id(), request);
    }

    /** O proprietario remove um editor, ou a propria pessoa sai do projeto. */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable UUID projectId,
                       @PathVariable UUID userId) {
        memberService.remove(projectId, user.id(), userId);
    }
}
