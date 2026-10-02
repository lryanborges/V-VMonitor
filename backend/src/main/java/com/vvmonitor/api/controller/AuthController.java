package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.LoginRequest;
import com.vvmonitor.api.dto.request.RegisterUserRequest;
import com.vvmonitor.api.dto.response.LoginResponse;
import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.service.AuthService;
import com.vvmonitor.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /** RF1, UC-01: cadastro de novo usuario. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    /** RF2, UC-02: autenticacao; devolve o token JWT. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
