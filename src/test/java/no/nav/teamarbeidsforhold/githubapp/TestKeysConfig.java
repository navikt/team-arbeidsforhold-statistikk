package no.nav.teamarbeidsforhold.githubapp;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.function.Supplier;

@TestConfiguration
public class TestKeysConfig {
    @Bean
    @Primary
    public Supplier<PrivateKey> generertTestNøkkel() throws Exception {
        return () -> {
            try {
                final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                return kpg.generateKeyPair().getPrivate();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
