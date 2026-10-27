package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.AddMemberRequest;
import com.vvmonitor.api.dto.response.MemberResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.AlreadyMemberException;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.OwnerCannotBeRemovedException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, JwtService.class})
class MemberControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String BASE = "/api/projects/" + PROJECT_ID + "/members";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.existsById(any())).thenReturn(true);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtService.issue(LOGGED_ID, "logado@exemplo.com").token());
    }

    @Test
    void listaPessoasComAcesso() throws Exception {
        when(memberService.list(PROJECT_ID, LOGGED_ID)).thenReturn(List.of(
                new MemberResponse(LOGGED_ID, "Ana", "ana@exemplo.com", MemberRole.OWNER, Instant.now()),
                new MemberResponse(UUID.randomUUID(), "Edu", "edu@exemplo.com", MemberRole.EDITOR, Instant.now())));

        mockMvc.perform(authenticated(get(BASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].email").value("edu@exemplo.com"));
    }

    @Test
    void adicionaPorEmail() throws Exception {
        when(memberService.add(eq(PROJECT_ID), eq(LOGGED_ID), any())).thenReturn(
                new MemberResponse(UUID.randomUUID(), "Carla", "carla@exemplo.com", MemberRole.EDITOR, Instant.now()));

        mockMvc.perform(authenticated(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"carla@exemplo.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EDITOR"));
        verify(memberService).add(PROJECT_ID, LOGGED_ID, new AddMemberRequest("carla@exemplo.com"));
    }

    @Test
    void emailInvalidoNaoChegaAoServico() throws Exception {
        mockMvc.perform(authenticated(post(BASE)).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        verifyNoInteractions(memberService);
    }

    @Test
    void semCadastroRetorna400NoCampoEJaMembro409() throws Exception {
        when(memberService.add(eq(PROJECT_ID), eq(LOGGED_ID), eq(new AddMemberRequest("ninguem@exemplo.com"))))
                .thenThrow(new InvalidFieldException("email", "Este e-mail não tem cadastro no ReqTrace Monitor."));
        mockMvc.perform(authenticated(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));

        when(memberService.add(eq(PROJECT_ID), eq(LOGGED_ID), eq(new AddMemberRequest("edu@exemplo.com"))))
                .thenThrow(new AlreadyMemberException("edu@exemplo.com"));
        mockMvc.perform(authenticated(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"edu@exemplo.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void removeAcesso() throws Exception {
        UUID other = UUID.randomUUID();
        mockMvc.perform(authenticated(delete(BASE + "/" + other))).andExpect(status().isNoContent());
        verify(memberService).remove(PROJECT_ID, LOGGED_ID, other);

        doThrow(new OwnerCannotBeRemovedException())
                .when(memberService).remove(PROJECT_ID, LOGGED_ID, LOGGED_ID);
        mockMvc.perform(authenticated(delete(BASE + "/" + LOGGED_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O proprietário não pode ser removido do projeto."));
    }
}
