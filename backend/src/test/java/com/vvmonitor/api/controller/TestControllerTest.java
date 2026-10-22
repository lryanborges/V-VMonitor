package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.TestRequest;
import com.vvmonitor.api.dto.response.ElementSummary;
import com.vvmonitor.api.dto.response.TestResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.TestService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestController.class)
@Import({SecurityConfig.class, JwtService.class})
class TestControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String BASE = "/api/projects/" + PROJECT_ID + "/tests";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TestService testService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.existsById(any())).thenReturn(true);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtService.issue(LOGGED_ID, "logado@exemplo.com").token());
    }

    private static TestResponse t1() {
        return new TestResponse(UUID.randomUUID(), "T1", "Login",
                List.of(new ElementSummary(UUID.randomUUID(), "RF2", ElementKind.FUNCTIONAL, "Login")),
                SubmissionStatus.DRAFT, Instant.now(), Instant.now());
    }

    @Test
    void listaTestes() throws Exception {
        when(testService.list(PROJECT_ID, LOGGED_ID)).thenReturn(List.of(t1()));

        mockMvc.perform(authenticated(get(BASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("T1"))
                .andExpect(jsonPath("$[0].covers[0].code").value("RF2"));
    }

    @Test
    void criaTeste() throws Exception {
        UUID rf2 = UUID.randomUUID();
        when(testService.create(eq(PROJECT_ID), eq(LOGGED_ID), any())).thenReturn(t1());

        mockMvc.perform(authenticated(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Login\",\"elementIds\":[\"" + rf2 + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("T1"));
        verify(testService).create(PROJECT_ID, LOGGED_ID, new TestRequest("Login", List.of(rf2)));
    }

    @Test
    void semRequisitoOuSemDescricaoRetorna400() throws Exception {
        mockMvc.perform(authenticated(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\",\"elementIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        verifyNoInteractions(testService);
    }

    @Test
    void excluiTeste() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(authenticated(delete(BASE + "/" + id))).andExpect(status().isNoContent());
        verify(testService).delete(PROJECT_ID, id, LOGGED_ID);
    }
}
