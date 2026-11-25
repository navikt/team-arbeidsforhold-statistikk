package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class PomId {
    @Size(max = 250)
    @NotNull
    @Column(name = "repo_full_name", nullable = false, length = 250)
    private String repoFullName;

    @Size(max = 250)
    @NotNull
    @Column(name = "path_in_repo", nullable = false, length = 250)
    private String pathInRepo;
}