package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.exception.UserNotFoundException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtService.class})
class UserControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.existsById(any())).thenReturn(true);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        String token = jwtService.issue(LOGGED_ID, "logado@exemplo.com").token();
        return request.header("Authorization", "Bearer " + token);
    }

    @Test
    void listaUsuariosSemExporSenha() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse(UUID.randomUUID(), "Ana", "ana@exemplo.com", Instant.now()),
                new UserResponse(UUID.randomUUID(), "Bruno", "bruno@exemplo.com", Instant.now())));

        mockMvc.perform(authenticated(get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void buscaUsuarioPorId() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenReturn(new UserResponse(id, "Ana", "ana@exemplo.com", Instant.now()));

        mockMvc.perform(authenticated(get("/api/users/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void meDevolveOUsuarioDoToken() throws Exception {
        when(userService.findById(LOGGED_ID))
                .thenReturn(new UserResponse(LOGGED_ID, "Logado", "logado@exemplo.com", Instant.now()));

        mockMvc.perform(authenticated(get("/api/users/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOGGED_ID.toString()));
    }

    @Test
    void usuarioInexistenteRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(authenticated(get("/api/users/{id}", id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário " + id + " não encontrado."));
    }

    @Test
    void uuidMalformadoRetorna400() throws Exception {
        mockMvc.perform(authenticated(get("/api/users/nao-e-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }

    // RNF1

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticação necessária. Envie um token válido."));
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioRemovidoRetorna401() throws Exception {
        when(userRepository.existsById(LOGGED_ID)).thenReturn(false);

        mockMvc.perform(authenticated(get("/api/users")))
                .andExpect(status().isUnauthorized());
    }
}
