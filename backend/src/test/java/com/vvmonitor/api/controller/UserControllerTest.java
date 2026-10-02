package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.domain.exception.UserNotFoundException;
import com.vvmonitor.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void listaUsuariosSemExporSenha() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse(UUID.randomUUID(), "Ana", "ana@exemplo.com", Instant.now()),
                new UserResponse(UUID.randomUUID(), "Bruno", "bruno@exemplo.com", Instant.now())));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void buscaUsuarioPorId() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenReturn(new UserResponse(id, "Ana", "ana@exemplo.com", Instant.now()));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void usuarioInexistenteRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário " + id + " não encontrado."));
    }

    @Test
    void uuidMalformadoRetorna400() throws Exception {
        mockMvc.perform(get("/api/users/nao-e-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }
}
