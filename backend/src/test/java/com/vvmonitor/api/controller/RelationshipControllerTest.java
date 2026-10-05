package com.vvmonitor.api.controller;

import com.vvmonitor.api.dto.response.ElementSummary;
import com.vvmonitor.api.dto.response.RelationshipResponse;
import com.vvmonitor.api.dto.response.RelationshipTypeResponse;
import com.vvmonitor.config.SecurityConfig;
import com.vvmonitor.domain.enums.ElementKind;
import com.vvmonitor.domain.enums.SubmissionStatus;
import com.vvmonitor.domain.exception.DuplicateRelationshipException;
import com.vvmonitor.domain.exception.RelationshipTypeAlreadyExistsException;
import com.vvmonitor.infra.repository.UserRepository;
import com.vvmonitor.infra.security.JwtService;
import com.vvmonitor.service.RelationshipService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelationshipController.class)
@Import({SecurityConfig.class, JwtService.class})
class RelationshipControllerTest {

    private static final UUID LOGGED_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String BASE = "/api/projects/" + PROJECT_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RelationshipService relationshipService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.existsById(any())).thenReturn(true);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtService.issue(LOGGED_ID, "logado@exemplo.com").token());
    }

    private static ElementSummary el(String code) {
        return new ElementSummary(UUID.randomUUID(), code, ElementKind.FUNCTIONAL, "desc");
    }

    @Test
    void listaTipos() throws Exception {
        when(relationshipService.listTypes(PROJECT_ID, LOGGED_ID)).thenReturn(List.of(
                new RelationshipTypeResponse(UUID.randomUUID(), "Dependência", false, false),
                new RelationshipTypeResponse(UUID.randomUUID(), "Valida", false, true)));

        mockMvc.perform(authenticated(get(BASE + "/relationship-types")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dependência"))
                .andExpect(jsonPath("$[1].custom").value(true));
    }

    @Test
    void criaRelacaoRetorna201() throws Exception {
        RelationshipResponse response = new RelationshipResponse(UUID.randomUUID(),
                new RelationshipTypeResponse(UUID.randomUUID(), "Dependência", false, false),
                el("RF7"), el("RF5"), SubmissionStatus.DRAFT, Instant.now());
        when(relationshipService.create(eq(PROJECT_ID), eq(LOGGED_ID), any())).thenReturn(response);

        mockMvc.perform(authenticated(post(BASE + "/relationships")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceId":"%s","targetId":"%s","typeId":"%s"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source.code").value("RF7"))
                .andExpect(jsonPath("$.target.code").value("RF5"))
                .andExpect(jsonPath("$.type.name").value("Dependência"));
    }

    @Test
    void camposAusentesRetornam400() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/relationships")).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("sourceId")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("targetId")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("typeId")));
    }

    @Test
    void duplicadaRetorna409() throws Exception {
        when(relationshipService.create(eq(PROJECT_ID), eq(LOGGED_ID), any()))
                .thenThrow(new DuplicateRelationshipException("RF7 já depende de RF5."));

        mockMvc.perform(authenticated(post(BASE + "/relationships")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceId":"%s","targetId":"%s","typeId":"%s"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("RF7 já depende de RF5. Registro duplicado não é permitido."));
    }

    @Test
    void tipoComNomeVazioRetorna400ERepetidoRetorna409() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/relationship-types")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());

        when(relationshipService.createType(eq(PROJECT_ID), eq(LOGGED_ID), any()))
                .thenThrow(new RelationshipTypeAlreadyExistsException("Conflito"));
        mockMvc.perform(authenticated(post(BASE + "/relationship-types")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"conflito\"}"))
                .andExpect(status().isConflict());
    }
}
