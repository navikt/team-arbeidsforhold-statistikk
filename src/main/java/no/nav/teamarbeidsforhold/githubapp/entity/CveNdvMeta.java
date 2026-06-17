package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "cve_ndv_meta")
public class CveNdvMeta {
    @Id
    @Size(max = 64)
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @NotNull
    @Column(name = "\"timestamp\"", nullable = false)
    private OffsetDateTime timestamp;


}