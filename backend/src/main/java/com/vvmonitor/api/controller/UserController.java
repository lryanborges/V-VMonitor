package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.infra.security.AuthenticatedUser;
import com.vvmonitor.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Dados do usuario dono do token. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.findById(principal.id());
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        return userService.findById(id);
    }
}
