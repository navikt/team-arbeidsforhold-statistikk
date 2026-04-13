package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "DEPLOYMENT")
public class Deployment {
    @EmbeddedId
    private DeploymentId id;

    @Column(name = "LAST_SEEN")
    private Instant lastSeen;


}