package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.LoginRequest;
import com.vvmonitor.api.dto.response.LoginResponse;
import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.exception.InvalidCredentialsException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Hash usado quando o e-mail nao existe, para que a resposta leve o mesmo tempo que uma senha errada. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /** UC-02, passos 4 e 5: valida as credenciais e emite o token de acesso. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(User.normalizeEmail(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        JwtService.IssuedToken token = jwtService.issue(user.get());
        return LoginResponse.bearer(token.token(), token.expiresAt(), UserResponse.from(user.get()));
    }
}
