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
@Table(name = "WORKLOAD")
public class Workload {
    @Id
    @Size(max = 250)
    @Column(name = "NAME", nullable = false, length = 250)
    private String name;

    @Size(max = 20)
    @Column(name = "WORKLOAD_TYPE", length = 20)
    private String workloadType;

    @Size(max = 250)
    @Column(name = "IMAGE", length = 250)
    private String image;


}