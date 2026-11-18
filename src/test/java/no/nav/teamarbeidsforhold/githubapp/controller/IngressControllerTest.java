package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.config.SikkerhetsKonfigurasjon;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.Ingress;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressGroup;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressOverview;
import no.nav.teamarbeidsforhold.githubapp.service.IngressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IngressController.class)
@Import(SikkerhetsKonfigurasjon.class)
class IngressControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private IngressService ingressService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void kreverInnlogging() throws Exception {
        for (final String sti : List.of("/", "/index.html", "/api/ingress", "/api/repo",
                "/api/me", "/api/vulnerability/critical")) {
            mvc.perform(get(sti)).andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(ingressService);
    }

    @Test
    void avviserAnonymeAdminKallUtenCsrf() throws Exception {
        for (final String handling : List.of("naisjobb", "nvdjobb", "liquibase/unlock")) {
            mvc.perform(post("/api/admin/" + handling)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(ingressService);
    }

    @Test
    void returnererGenerertKontraktForInnloggetBruker() throws Exception {
        when(jwtDecoder.decode("test-token")).thenReturn(Jwt.withTokenValue("test-token")
                .header("alg", "RS256").subject("test-user").build());
        final var ingress = new Ingress("https://app.nav.no/path", "app", "prod-gcp", "")
                .repository("navikt/repo");
        final var gruppe = new IngressGroup(IngressGroup.ExposureEnum.INTERNET, "Internett", "Beskrivelse",
                List.of(ingress));
        when(ingressService.oversikt()).thenReturn(new IngressOverview(List.of(gruppe), 1));

        mvc.perform(get("/api/ingress").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].exposure").value("INTERNET"))
                .andExpect(jsonPath("$.groups[0].ingresses[0].url").value("https://app.nav.no/path"))
                .andExpect(jsonPath("$.groups[0].ingresses[0].repository").value("navikt/repo"))
                .andExpect(jsonPath("$.deploymentsWithoutManifest").value(1));
    }
}
