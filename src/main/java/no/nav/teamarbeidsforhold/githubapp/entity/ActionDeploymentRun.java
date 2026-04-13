package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ACTION_DEPLOYMENT_RUN")
public class ActionDeploymentRun {
    @Id
    @Size(max = 250)
    @Column(name = "REPO_FULL_NAME", nullable = false, length = 250)
    private String repoFullName;

    @Size(max = 250)
    @Column(name = "IMAGE", length = 250)
    private String image;


}