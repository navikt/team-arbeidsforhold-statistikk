package no.nav.teamarbeidsforhold.githubapp.entity;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "pom")
public class Pom {
@jakarta.persistence.EmbeddedId
private no.nav.teamarbeidsforhold.githubapp.entity.PomId id;

@jakarta.persistence.MapsId("repoFullName")
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "repo_full_name", nullable = false)
private no.nav.teamarbeidsforhold.githubapp.entity.Repo repoFullName;

@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.SQLXML)
@jakarta.persistence.Column(name = "contents")
private java.lang.String contents;

@jakarta.persistence.Column(name = "last_commit_timestamp")
private java.time.Instant lastCommitTimestamp;

@jakarta.persistence.Column(name = "last_fetch_timestamp")
private java.time.Instant lastFetchTimestamp;



}