package no.nav.teamarbeidsforhold.githubapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "cve_nvd")
public class CveNvd {
    @Id
    @Size(max = 32)
    @Column(name = "cve_id", nullable = false, length = 32)
    private String cveId;

    @MapsId
    @OneToOne
    @JoinColumn(name="cve_id")
    private Vulnerability vulnerability;

    @NotNull
    @Column(name = "published", nullable = false)
    private Instant published;

    @NotNull
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @Size(max = 10)
    @Column(name = "cvss_version", length = 10)
    private String cvssVersion;

    @Size(max = 255)
    @Column(name = "vector_string")
    private String vectorString;

    @Column(name = "base_score", precision = 3, scale = 1)
    private BigDecimal baseScore;

    @Size(max = 16)
    @Column(name = "base_severity", length = 16)
    private String baseSeverity;

    @Size(max = 16)
    @Column(name = "attack_vector", length = 16)
    private String attackVector;

    @Size(max = 16)
    @Column(name = "attack_complexity", length = 16)
    private String attackComplexity;

    @Size(max = 16)
    @Column(name = "attack_requirements", length = 16)
    private String attackRequirements;

    @Size(max = 16)
    @Column(name = "privileges_required", length = 16)
    private String privilegesRequired;

    @Size(max = 16)
    @Column(name = "user_interaction", length = 16)
    private String userInteraction;

    @Size(max = 8)
    @Column(name = "vc", length = 8)
    private String vc;

    @Size(max = 8)
    @Column(name = "vi", length = 8)
    private String vi;

    @Size(max = 8)
    @Column(name = "va", length = 8)
    private String va;

    @Size(max = 8)
    @Column(name = "sc", length = 8)
    private String sc;

    @Size(max = 8)
    @Column(name = "si", length = 8)
    private String si;

    @Size(max = 8)
    @Column(name = "sa", length = 8)
    private String sa;

    @Size(max = 8)
    @Column(name = "exploit_maturity", length = 8)
    private String exploitMaturity;

    @Size(max = 16)
    @Column(name = "modified_attack_vector", length = 16)
    private String modifiedAttackVector;

    @Size(max = 16)
    @Column(name = "modified_attack_complexity", length = 16)
    private String modifiedAttackComplexity;

    @Size(max = 16)
    @Column(name = "modified_attack_requirements", length = 16)
    private String modifiedAttackRequirements;

    @Size(max = 16)
    @Column(name = "modified_privileges_required", length = 16)
    private String modifiedPrivilegesRequired;

    @Size(max = 16)
    @Column(name = "modified_user_interaction", length = 16)
    private String modifiedUserInteraction;

    @Size(max = 8)
    @Column(name = "modified_vc", length = 8)
    private String modifiedVc;

    @Size(max = 8)
    @Column(name = "modified_vi", length = 8)
    private String modifiedVi;

    @Size(max = 8)
    @Column(name = "modified_va", length = 8)
    private String modifiedVa;


}