package no.nav.teamarbeidsforhold.githubapp;

import liquibase.integration.spring.SpringLiquibase;
import no.nav.teamarbeidsforhold.githubapp.components.GithubOppdateringsKø;
import no.nav.teamarbeidsforhold.githubapp.components.KopierNaisApiData;
import no.nav.teamarbeidsforhold.githubapp.components.KopierNvdCveData;
import no.nav.teamarbeidsforhold.githubapp.components.LederUtpekerServerSideEventHåndterer;
import no.nav.teamarbeidsforhold.githubapp.components.SlackSender;
import no.nav.teamarbeidsforhold.githubapp.components.TidsstyrteHandlinger;
import com.slack.api.Slack;
import no.nav.teamarbeidsforhold.githubapp.controller.AdminController;
import no.nav.teamarbeidsforhold.githubapp.controller.LokalRepoController;
import no.nav.teamarbeidsforhold.githubapp.controller.RepoController;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.location=file:src/main/resources/application.yaml",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class LokalOppstartTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationContext kontekst;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void lastEksempeldata() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("local/eksempeldata.sql"));
        }
    }

    @Test
    void viser_eksempeldata_uten_innlogging() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(get("/api/me")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ukjent"));
        mvc.perform(get("/api/repo")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("eksempel/eksempel-api", "eksempel/eksempel-web")));
        mvc.perform(get("/api/repo/eksempel-api")).andExpect(status().isOk())
                .andExpect(jsonPath("$.environments", hasItems("prod-gcp", "dev-gcp")));
        mvc.perform(get("/api/ingress")).andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].ingresses.length()").value(3))
                .andExpect(jsonPath("$.groups[0].ingresses[*].url",
                        hasItems("https://api.example.test", "https://api-q1.example.test", "https://web.example.test")));
        mvc.perform(get("/api/vulnerability/critical")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("CVE-2099-90001"))
                .andExpect(jsonPath("$[0].affectedRepositories").value(2));
    }

    @Test
    void oppretter_ikke_eksterne_integrasjoner_eller_admin() {
        for (final Class<?> type : List.of(GitHub.class, GithubOppdateringsKø.class,
                KopierNaisApiData.class, KopierNvdCveData.class, LederUtpekerServerSideEventHåndterer.class,
                AdminController.class, RepoController.class, JwtDecoder.class,
                Slack.class, SlackSender.class, TidsstyrteHandlinger.class)) {
            assertTrue(kontekst.getBeansOfType(type).isEmpty(), type.getSimpleName());
        }
        assertNotNull(kontekst.getBean(LokalRepoController.class));
        assertFalse(kontekst.containsBean("naisToken"));
    }

    @Test
    void avviser_admin_også_med_gyldig_csrf_token() throws Exception {
        final MockHttpSession session = new MockHttpSession();
        final MvcResult resultat = mvc.perform(get("/api/me").session(session)).andExpect(status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) resultat.getRequest().getAttribute(CsrfToken.class.getName());
        final String verdi = token.getToken();
        for (final String handling : List.of("naisjobb", "nvdjobb", "liquibase/unlock")) {
            mvc.perform(post("/api/admin/" + handling)).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/" + handling).session(session).param(token.getParameterName(), verdi))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void gjentatt_innlasting_beholder_egne_eksempler() throws Exception {
        jdbc.update("INSERT INTO repo (full_name, clone_url) VALUES (?, ?)",
                "egne/egen-side", "https://github.com/egne/egen-side.git");
        jdbc.update("UPDATE repo SET clone_url = ? WHERE full_name = ?",
                "https://github.com/egne/tilpasset.git", "eksempel/eksempel-api");
        lastEksempeldata();
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM repo WHERE full_name LIKE 'eksempel/%'", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM deployment", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM deployment_vulnerability", Integer.class));
        assertEquals("https://github.com/egne/egen-side.git",
                jdbc.queryForObject("SELECT clone_url FROM repo WHERE full_name = ?", String.class, "egne/egen-side"));
        assertEquals("https://github.com/egne/tilpasset.git",
                jdbc.queryForObject("SELECT clone_url FROM repo WHERE full_name = ?", String.class, "eksempel/eksempel-api"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LokalTestDatabase {
        @Bean
        DataSource dataSource() {
            final JdbcDataSource database = new JdbcDataSource();
            database.setURL("jdbc:h2:mem:lokal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            return database;
        }

        @Bean
        SpringLiquibase liquibase(final DataSource dataSource) {
            final SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:db/changelog-master.yaml");
            return liquibase;
        }
    }
}
