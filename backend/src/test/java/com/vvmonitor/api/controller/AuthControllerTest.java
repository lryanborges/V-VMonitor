package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.domain.exception.EmailAlreadyRegisteredException;
import com.vvmonitor.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String URL = "/api/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void cadastroValidoRetorna201SemExporSenha() throws Exception {
        when(userService.register(any())).thenReturn(
                new UserResponse(UUID.randomUUID(), "Ana", "ana@exemplo.com", Instant.now()));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ana","email":"ana@exemplo.com","password":"senha1234","passwordConfirmation":"senha1234"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void senhaCurtaRetorna400ComErroNoCampo() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ana","email":"ana@exemplo.com","password":"curta","passwordConfirmation":"curta"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));

        verify(userService, never()).register(any());
    }

    @Test
    void confirmacaoDiferenteRetorna400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ana","email":"ana@exemplo.com","password":"senha1234","passwordConfirmation":"outra1234"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("passwordConfirmed")));
    }

    @Test
    void emailInvalidoECamposVaziosRetornam400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"","email":"nao-e-email","password":"senha1234","passwordConfirmation":"senha1234"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
    }

    @Test
    void emailJaCadastradoRetorna409() throws Exception {
        when(userService.register(any())).thenThrow(new EmailAlreadyRegisteredException("ana@exemplo.com"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ana","email":"ana@exemplo.com","password":"senha1234","passwordConfirmation":"senha1234"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O e-mail ana@exemplo.com já está cadastrado."));
    }

    @Test
    void corpoMalformadoRetorna400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
                .andExpect(status().isBadRequest());
    }
}
