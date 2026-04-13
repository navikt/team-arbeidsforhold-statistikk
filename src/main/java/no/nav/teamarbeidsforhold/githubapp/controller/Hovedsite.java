package no.nav.teamarbeidsforhold.githubapp.controller;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class Hovedsite {

    @GetMapping({"/{ignored:[^.]*}", "/**/{ignored:[^.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
