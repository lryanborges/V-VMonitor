package com.vvmonitor.service;

import com.vvmonitor.api.dto.request.RegisterUserRequest;
import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.domain.entity.User;
import com.vvmonitor.domain.exception.EmailAlreadyRegisteredException;
import com.vvmonitor.domain.exception.UserNotFoundException;
import com.vvmonitor.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registraUsuarioComSenhaEmHashEEmailNormalizado() {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.register(
                new RegisterUserRequest("  Ana  ", " Ana@Exemplo.COM ", "senha1234", "senha1234"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        User user = saved.getValue();

        assertThat(user.getName()).isEqualTo("Ana");
        assertThat(user.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(user.getPasswordHash()).isNotEqualTo("senha1234").startsWith("$2");
        assertThat(passwordEncoder.matches("senha1234", user.getPasswordHash())).isTrue();
        assertThat(response.email()).isEqualTo("ana@exemplo.com");
    }

    @Test
    void rejeitaEmailJaCadastradoSemSalvar() {
        when(userRepository.existsByEmail("ana@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(
                new RegisterUserRequest("Ana", "ANA@exemplo.com", "senha1234", "senha1234")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void buscaPorIdInexistenteLancaUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void cadastroConcorrenteComMesmoEmailViraEmailJaCadastrado() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("ux_users_email"));

        assertThatThrownBy(() -> userService.register(
                new RegisterUserRequest("Ana", "ana@exemplo.com", "senha1234", "senha1234")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
