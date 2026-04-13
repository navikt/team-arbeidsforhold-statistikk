package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "REPO")
public class Repo {
    @Id
    @Size(max = 250)
    @Column(name = "FULL_NAME", nullable = false, length = 250)
    private String fullName;

    @Size(max = 250)
    @Column(name = "CLONE_URL", length = 250)
    private String cloneUrl;

    @Column(name = "LAST_SEEN")
    private Instant lastSeen;


}