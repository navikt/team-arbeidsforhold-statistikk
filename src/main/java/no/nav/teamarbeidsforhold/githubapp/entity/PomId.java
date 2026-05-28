package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class PomId implements Serializable {
    private static final long serialVersionUID = -335171151987863445L;
    @Size(max = 250)
    @NotNull
    @Column(name = "repo_full_name", nullable = false, length = 250)
    private String repoFullName;

    @Size(max = 250)
    @NotNull
    @Column(name = "path_in_repo", nullable = false, length = 250)
    private String pathInRepo;


}