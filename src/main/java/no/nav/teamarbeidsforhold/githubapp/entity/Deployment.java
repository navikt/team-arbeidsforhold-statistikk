package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "deployment")
public class Deployment {
    @EmbeddedId
    private DeploymentId id;

    @MapsId("workloadName")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_name", nullable = false)
    private Workload workloadName;

    @Size(max = 20)
    @NotNull
    @Column(name = "workload_type", nullable = false, length = 20)
    private String workloadType;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Size(max = 250)
    @Column(name = "last_deployer", length = 250)
    private String lastDeployer;

    @Size(max = 250)
    @Column(name = "last_commit", length = 250)
    private String lastCommit;

    @Column(name = "last_deploy_time")
    private Instant lastDeployTime;

    @Column(name = "manifest", length = Integer.MAX_VALUE)
    private String manifest;


}