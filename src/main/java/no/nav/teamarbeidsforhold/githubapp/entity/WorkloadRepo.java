package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "WORKLOAD_REPO")
public class WorkloadRepo {
    @EmbeddedId
    private WorkloadRepoId id;

    @MapsId("workloadName")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "WORKLOAD_NAME", nullable = false)
    private Workload workloadName;

    @MapsId("repoFullName")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "REPO_FULL_NAME", nullable = false)
    private Repo repoFullName;

    @Column(name = "LAST_SEEN")
    private Instant lastSeen;


}