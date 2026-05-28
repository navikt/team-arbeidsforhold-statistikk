package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "pom")
public class Pom {
    @EmbeddedId
    private PomId id;

    @MapsId("repoFullName")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repo_full_name", nullable = false)
    private Repo repoFullName;

    @JdbcTypeCode(SqlTypes.SQLXML)
    @Column(name = "contents")
    private String contents;

    @Column(name = "last_commit_timestamp")
    private Instant lastCommitTimestamp;

    @Column(name = "last_fetch_timestamp")
    private Instant lastFetchTimestamp;


}