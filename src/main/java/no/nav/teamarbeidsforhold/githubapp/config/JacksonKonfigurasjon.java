package no.nav.teamarbeidsforhold.githubapp.config;

import no.nav.teamarbeidsforhold.githubapp.qualifier.YamlParsing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class JacksonKonfigurasjon {
    @Bean
    @YamlParsing
    public ObjectMapper objectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }
}
