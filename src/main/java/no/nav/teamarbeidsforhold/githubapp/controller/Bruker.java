package no.nav.teamarbeidsforhold.githubapp.controller;

import no.nav.teamarbeidsforhold.githubapp.generert.modell.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController("")
public final class Bruker {
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/me",
            produces = {"application/json"}
    )
    public ResponseEntity<User> getBruker(@AuthenticationPrincipal UserDetails bruker) {
        return ok(new User(bruker.getUsername()));
    }
}
