package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.modell.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController()
@RequestMapping("/api")
public final class Bruker {
    @RequestMapping(method = RequestMethod.GET, value = "/me", produces = {"application/json"})
    public ResponseEntity<User> getBruker(Authentication bruker) {
        final User user = new User(bruker.getName());
        if (bruker.getPrincipal() != null) {
            user.setDebug("Klasse: " + bruker.getPrincipal().getClass());
        }
        return ok(user);
    }
}
