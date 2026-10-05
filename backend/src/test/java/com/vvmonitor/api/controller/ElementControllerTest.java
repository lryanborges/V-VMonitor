package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.ElementResponse;
import com.vvmonitor.api.dto.response.NextCodeResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.MemberRole;
import com.vvmonitor.domain.enums.Priority;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.domain.exception.InvalidFieldException;
import com.vvmonitor.domain.exception.ProjectAccessDeniedException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.api.dto.response.DeleteImpactResponse;
import com.vvmonitor.service.ElementRemovalService;
import com.vvmonitor.service.ElementService;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ElementController.class)
@Import({SecurityConfig.class, JwtService.class})
class ElementControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String URL = "/api/projects/" + PROJECT_ID + "/elements";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ElementService elementService;

    @MockitoBean
    private ElementRemovalService removalService;

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

    private static ElementResponse rf(String code) {
        return new ElementResponse(UUID.randomUUID(), code, ElementKind.FUNCTIONAL, "desc", Priority.MANDATORY,
                SubmissionStatus.DRAFT, 0, 0, Instant.now(), Instant.now());
    }

    @Test
    void listaElementos() throws Exception {
        when(elementService.list(PROJECT_ID, LOGGED_ID)).thenReturn(List.of(rf("RF1")));

        mockMvc.perform(authenticated(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("RF1"))
                .andExpect(jsonPath("$[0].priority").value("MANDATORY"))
                .andExpect(jsonPath("$[0].submissionStatus").value("DRAFT"));
    }

    @Test
    void cadastraRetorna201() throws Exception {
        when(elementService.create(eq(PROJECT_ID), eq(LOGGED_ID), any())).thenReturn(rf("RF21"));

        mockMvc.perform(authenticated(post(URL)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"FUNCTIONAL","description":"Exportar CSV","priority":"MANDATORY"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("RF21"));
    }

    @Test
    void semTipoEDescricaoRetorna400() throws Exception {
        mockMvc.perform(authenticated(post(URL)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"  "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("kind")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("description")));
    }

    @Test
    void prioridadeInconsistenteRetorna400NoCampo() throws Exception {
        when(elementService.create(eq(PROJECT_ID), eq(LOGGED_ID), any()))
                .thenThrow(new InvalidFieldException("priority", "A prioridade é obrigatória para requisitos."));

        mockMvc.perform(authenticated(post(URL)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"FUNCTIONAL","description":"x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priority"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("A prioridade é obrigatória para requisitos."));
    }

    @Test
    void visualizadorRecebe403() throws Exception {
        when(elementService.create(eq(PROJECT_ID), eq(LOGGED_ID), any()))
                .thenThrow(new ProjectAccessDeniedException(MemberRole.EDITOR));

        mockMvc.perform(authenticated(post(URL)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"BUSINESS_RULE","description":"x"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void previaDoProximoCodigo() throws Exception {
        when(elementService.nextCode(PROJECT_ID, LOGGED_ID, ElementKind.BUSINESS_RULE))
                .thenReturn(new NextCodeResponse(ElementKind.BUSINESS_RULE, "RN5"));

        mockMvc.perform(authenticated(get(URL + "/next-code").param("kind", "BUSINESS_RULE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RN5"));
    }

    @Test
    void tipoInvalidoOuAusenteNaPreviaRetorna400() throws Exception {
        mockMvc.perform(authenticated(get(URL + "/next-code").param("kind", "XYZ")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(authenticated(get(URL + "/next-code")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editaRetorna200() throws Exception {
        UUID id = UUID.randomUUID();
        when(elementService.update(eq(PROJECT_ID), eq(id), eq(LOGGED_ID), any())).thenReturn(rf("RF3"));

        mockMvc.perform(authenticated(put(URL + "/" + id)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"Nova descrição","priority":"OPTIONAL"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RF3"));
    }

    @Test
    void edicaoSemDescricaoRetorna400() throws Exception {
        mockMvc.perform(authenticated(put(URL + "/" + UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("description")));
    }

    @Test
    void impactoDaExclusao() throws Exception {
        UUID id = UUID.randomUUID();
        when(removalService.impact(PROJECT_ID, id, LOGGED_ID)).thenReturn(new DeleteImpactResponse(
                DeleteImpactResponse.ImpactLevel.HIGH, List.of(), List.of(), List.of(), List.of(), 0));

        mockMvc.perform(authenticated(get(URL + "/" + id + "/delete-impact")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("HIGH"));
    }

    @Test
    void exclusaoRetorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(authenticated(delete(URL + "/" + id)))
                .andExpect(status().isNoContent());
        verify(removalService).delete(PROJECT_ID, id, LOGGED_ID);
    }
}
