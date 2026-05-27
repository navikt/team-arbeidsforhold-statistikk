package no.nav.teamarbeidsforhold.githubapp.entity;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode@jakarta.persistence.Embeddable
public class PomId {
@jakarta.validation.constraints.Size(max = 250)
@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "repo_full_name", nullable = false, length = 250)
private java.lang.String repoFullName;

@jakarta.validation.constraints.Size(max = 250)
@jakarta.validation.constraints.NotNull
@jakarta.persistence.Column(name = "path_in_repo", nullable = false, length = 250)
private java.lang.String pathInRepo;



}