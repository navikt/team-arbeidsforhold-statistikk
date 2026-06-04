package no.nav.teamarbeidsforhold.githubapp.config;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

@Configuration
public class MavenConfiguration {

    @Bean
    public RepositorySystem repositorySystem() {
        return new RepositorySystemSupplier().get();
    }

    @Bean(destroyMethod = "close")
    public RepositorySystemSession.CloseableSession repositorySystemSession(RepositorySystem repositorySystem) {
        RepositorySystemSession.SessionBuilder builder = new SessionBuilderSupplier(repositorySystem).get();
        LocalRepository localRepository = new LocalRepository(
                Path.of("target/local-repo").toAbsolutePath()
        );

        builder.setLocalRepositoryManager(
                repositorySystem.newLocalRepositoryManager(builder.build(), localRepository)
        );

        builder.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        builder.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);

        builder.setTransferListener(null);
        builder.setRepositoryListener(null);

        return builder.build();
    }

    @Bean
    public List<RemoteRepository> remoteRepositories() {

        RemoteRepository internal = new RemoteRepository.Builder(
                "internal",
                "default",
                "https://repo.adeo.no/repository/maven-public/"
        )
                .setReleasePolicy(new RepositoryPolicy(
                        true,
                        RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_FAIL
                ))
                .setSnapshotPolicy(new RepositoryPolicy(
                        true,
                        RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":10",
                        RepositoryPolicy.CHECKSUM_POLICY_WARN
                ))
                .build();

        return List.of(internal);
    }
}
