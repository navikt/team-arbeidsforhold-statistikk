package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.MeApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public final class BrukerController implements MeApi {
    @Override
    public ResponseEntity<User> apiMeGet() {
        final Authentication bruker = SecurityContextHolder.getContext().getAuthentication();
        final User user = new User(bruker != null ? bruker.getName() : "null");
        if (bruker != null && bruker.getPrincipal() != null) {
            user.setDebug("Klasse: " + bruker.getPrincipal().getClass());
        }
        return ok(user);
    }
}
