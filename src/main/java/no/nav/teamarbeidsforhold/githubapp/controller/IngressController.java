package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.IngressApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.IngressOverview;
import no.nav.teamarbeidsforhold.githubapp.service.IngressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IngressController implements IngressApi {
    private final IngressService ingressService;

    public IngressController(final IngressService ingressService) {
        this.ingressService = ingressService;
    }

    @Override
    public ResponseEntity<IngressOverview> apiIngressGet() {
        return ResponseEntity.ok(ingressService.oversikt());
    }
}
