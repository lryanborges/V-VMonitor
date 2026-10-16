package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.request.CreateVersionRequest;
import com.vvmonitor.api.dto.response.ProjectResponse;
import com.vvmonitor.api.dto.response.VersionSummaryResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.NothingToSubmitException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.VersionNotFoundException;
import com.vvmonitor.domain.version.ChangeSummary;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.VersionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VersionController.class)
@Import({SecurityConfig.class, JwtService.class})
class VersionControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String BASE = "/api/projects/" + PROJECT_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private VersionService versionService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.existsById(any())).thenReturn(true);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtService.issue(LOGGED_ID, "logado@exemplo.com").token());
    }

    private static VersionSummaryResponse version(int number, String label) {
        return new VersionSummaryResponse(UUID.randomUUID(), number, label,
                new ProjectResponse.PersonSummary(LOGGED_ID, "Logado"), Instant.now(), 12, 8,
                new ChangeSummary(3, 1, 2));
    }

    @Test
    void submeteCriandoAVersaoComNome() throws Exception {
        when(versionService.create(eq(PROJECT_ID), eq(LOGGED_ID), any())).thenReturn(version(3, "Revisão"));

        mockMvc.perform(authenticated(post(BASE + "/versions"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Revisão\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(3))
                .andExpect(jsonPath("$.label").value("Revisão"))
                .andExpect(jsonPath("$.createdBy.name").value("Logado"))
                .andExpect(jsonPath("$.changes.removed").value(2));
        verify(versionService).create(PROJECT_ID, LOGGED_ID, new CreateVersionRequest("Revisão"));
    }

    @Test
    void nomeMuitoLongoRetorna400() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/versions"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("label"));
        verifyNoInteractions(versionService);
    }

    @Test
    void semMudancasRetorna409EVisualizador403() throws Exception {
        when(versionService.create(eq(PROJECT_ID), eq(LOGGED_ID), any())).thenThrow(new NothingToSubmitException());
        mockMvc.perform(authenticated(post(BASE + "/versions")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Não há alterações desde a última versão."));

        UUID other = UUID.randomUUID();
        when(versionService.create(eq(other), eq(LOGGED_ID), any()))
                .thenThrow(new ProjectAccessDeniedException(MemberRole.EDITOR));
        mockMvc.perform(authenticated(post("/api/projects/" + other + "/versions"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listaVersoesEVersaoInexistenteRetorna404() throws Exception {
        when(versionService.list(PROJECT_ID, LOGGED_ID)).thenReturn(List.of(version(2, null), version(1, "Inicial")));
        mockMvc.perform(authenticated(get(BASE + "/versions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[1].label").value("Inicial"));

        when(versionService.find(PROJECT_ID, 9, LOGGED_ID)).thenThrow(new VersionNotFoundException(9));
        mockMvc.perform(authenticated(get(BASE + "/versions/9")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Versão v9 não encontrada."));
    }
}
