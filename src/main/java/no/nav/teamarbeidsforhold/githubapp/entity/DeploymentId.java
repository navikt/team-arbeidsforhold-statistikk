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
public class DeploymentId implements Serializable {
    private static final long serialVersionUID = 7365486554740526394L;
    @Size(max = 250)
    @NotNull
    @Column(name = "WORKLOAD_NAME", nullable = false, length = 250)
    private String workloadName;

    @Size(max = 20)
    @NotNull
    @Column(name = "WORKLOAD_TYPE", nullable = false, length = 20)
    private String workloadType;

    @Size(max = 20)
    @NotNull
    @Column(name = "ENVIRONMENT", nullable = false, length = 20)
    private String environment;


}