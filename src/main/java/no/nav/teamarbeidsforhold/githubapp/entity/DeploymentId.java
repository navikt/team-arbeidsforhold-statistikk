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
public class DeploymentId {
    @Size(max = 250)
    @NotNull
    @Column(name = "workload_name", nullable = false, length = 250)
    private String workloadName;

    @Size(max = 2)
    @NotNull
    @Column(name = "suffix", nullable = false, length = 2)
    private String suffix;

    @Size(max = 20)
    @NotNull
    @Column(name = "environment", nullable = false, length = 20)
    private String environment;


}