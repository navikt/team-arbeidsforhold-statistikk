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
public class WorkloadRepoId {
    @Size(max = 250)
    @NotNull
    @Column(name = "WORKLOAD_NAME", nullable = false, length = 250)
    private String workloadName;

    @Size(max = 250)
    @NotNull
    @Column(name = "REPO_FULL_NAME", nullable = false, length = 250)
    private String repoFullName;


}