package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.LoginResponse;
import com.vvmonitor.api.dto.response.UserResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.exception.EmailAlreadyRegisteredException;
import com.vvmonitor.domain.exception.InvalidCredentialsException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.AuthService;
import com.vvmonitor.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
@Import({SecurityConfig.class, JwtService.class})
class AuthControllerTest {

    private static final String URL = "/api/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void loginValidoRetorna200ComToken() throws Exception {
        UserResponse user = new UserResponse(UUID.randomUUID(), "Ana", "ana@exemplo.com", Instant.now());
        when(authService.login(any())).thenReturn(LoginResponse.bearer("token-abc", Instant.now(), user));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"ana@exemplo.com","password":"senha1234"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("ana@exemplo.com"));
    }

    @Test
    void loginComCredenciaisInvalidasRetorna401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"ana@exemplo.com","password":"errada123"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginSemCamposRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

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
