package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.LoginRequest;
import com.vvmonitor.api.dto.response.LoginResponse;
import com.vvmonitor.config.JwtProperties;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.exception.InvalidCredentialsException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private JwtService jwtService;
    private AuthService authService;
    private User ana;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(new JwtProperties("segredo-de-teste-com-mais-de-32-bytes-123456", Duration.ofHours(2)));
        authService = new AuthService(userRepository, encoder, jwtService);

        ana = new User("Ana", "ana@exemplo.com", encoder.encode("senha1234"));
        ReflectionTestUtils.setField(ana, "id", UUID.randomUUID());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(ana));
    }

    @Test
    void loginValidoDevolveTokenDoUsuario() {
        LoginResponse response = authService.login(new LoginRequest(" ANA@exemplo.com ", "senha1234"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("ana@exemplo.com");
        assertThat(jwtService.parse(response.accessToken()))
                .hasValueSatisfying(user -> assertThat(user.id()).isEqualTo(ana.getId()));
    }

    @Test
    void senhaErradaEhRejeitada() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@exemplo.com", "errada123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    void emailInexistenteTemAMesmaMensagemDeSenhaErrada() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@exemplo.com", "senha1234")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }
}
