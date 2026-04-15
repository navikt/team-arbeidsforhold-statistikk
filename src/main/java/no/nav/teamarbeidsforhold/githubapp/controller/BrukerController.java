package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.api.MeApi;
import no.nav.teamarbeidsforhold.githubapp.generert.modell.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
public final class BrukerController implements MeApi {
    @Override
    public ResponseEntity<User> apiMeGet() {
        final Authentication bruker = SecurityContextHolder.getContext().getAuthentication();
        final User user;
        if (bruker != null && bruker.getPrincipal() instanceof Jwt jwt) {
            user = new User(jwt.getSubject());
            user.setDebug("claims=" + jwt.getClaims() + ",audience=" + jwt.getAudience());
        } else {
            user = new User("Ukjent");
            if (bruker == null) {
                user.setDebug("null authentication");
            } else {
                user.setDebug("ukjent klasse " + bruker.getClass().getSimpleName());
            }
        }
        return ok(user);
    }
}
