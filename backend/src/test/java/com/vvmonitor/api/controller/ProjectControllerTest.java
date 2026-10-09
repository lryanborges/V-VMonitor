package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.ProjectResponse;
import com.vvmonitor.api.dto.response.SubmissionResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.exception.NothingToSubmitException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.domain.exception.ProjectNotFoundException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.ProjectService;
import com.vvmonitor.service.SubmissionService;
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

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtService.class})
class ProjectControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private SubmissionService submissionService;

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

    private static ProjectResponse sample(UUID id) {
        return new ProjectResponse(id, "Agenda clínica", null, MemberRole.OWNER,
                new ProjectResponse.PersonSummary(LOGGED_ID, "Logado"),
                List.of(new ProjectResponse.MemberSummary(LOGGED_ID, "Logado", MemberRole.OWNER)),
                ProjectResponse.Stats.of(0, 0, 0, 0), null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void listaProjetosDoUsuarioLogado() throws Exception {
        when(projectService.listAccessible(LOGGED_ID)).thenReturn(List.of(sample(UUID.randomUUID())));

        mockMvc.perform(authenticated(get("/api/projects")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Agenda clínica"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].owner.name").value("Logado"))
                .andExpect(jsonPath("$[0].stats.coverage").value(0));
    }

    @Test
    void criaProjetoRetorna201() throws Exception {
        when(projectService.create(eq(LOGGED_ID), any())).thenReturn(sample(UUID.randomUUID()));

        mockMvc.perform(authenticated(post("/api/projects")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Agenda clínica","description":"Consultas e lembretes"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Agenda clínica"));
    }

    @Test
    void projetoSemNomeRetorna400() throws Exception {
        mockMvc.perform(authenticated(post("/api/projects")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"   ","description":"x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));

        verify(projectService, never()).create(any(), any());
    }

    @Test
    void projetoInacessivelRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(projectService.findById(id, LOGGED_ID)).thenThrow(new ProjectNotFoundException(id));

        mockMvc.perform(authenticated(get("/api/projects/{id}", id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projeto " + id + " não encontrado."));
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submeteModelo() throws Exception {
        UUID id = UUID.randomUUID();
        when(submissionService.submit(id, LOGGED_ID)).thenReturn(new SubmissionResponse(7, 4, Instant.now(),
                new ProjectResponse.PersonSummary(LOGGED_ID, "Logado")));

        mockMvc.perform(authenticated(post("/api/projects/{id}/submit", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements").value(7))
                .andExpect(jsonPath("$.relationships").value(4))
                .andExpect(jsonPath("$.submittedBy.name").value("Logado"));
    }

    @Test
    void submeterSemPendenciasRetorna409EVisualizador403() throws Exception {
        UUID id = UUID.randomUUID();
        when(submissionService.submit(id, LOGGED_ID)).thenThrow(new NothingToSubmitException());
        mockMvc.perform(authenticated(post("/api/projects/{id}/submit", id)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Não há alterações para submeter."));

        UUID other = UUID.randomUUID();
        when(submissionService.submit(other, LOGGED_ID)).thenThrow(new ProjectAccessDeniedException(MemberRole.EDITOR));
        mockMvc.perform(authenticated(post("/api/projects/{id}/submit", other)))
                .andExpect(status().isForbidden());
    }
}
