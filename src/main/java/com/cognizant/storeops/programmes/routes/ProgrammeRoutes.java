package com.cognizant.storeops.programmes.routes;

import com.cognizant.storeops.programmes.model.*;
import com.cognizant.storeops.programmes.service.ProgrammeService;
import com.cognizant.storeops.shared.auth.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/programmes")
public class ProgrammeRoutes {
    private final ProgrammeService service;

    public ProgrammeRoutes(ProgrammeService s) {
        service = s;
    }

    private RequestContext c(String u, String s, String r) {
        return RequestContexts.of(u, s, r);
    }

    @GetMapping
    public List<Project> list(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r) {
        return service.list(c(u, s, r));
    }

    @PostMapping
    public ResponseEntity<Project> create(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @Valid @RequestBody CreateProjectRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(c(u, s, r), body));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMember> add(@RequestHeader("X-User-Id") String u,
            @RequestHeader("X-Store-Id") String s, @RequestHeader("X-User-Role") String r, @PathVariable String id,
            @Valid @RequestBody AddMemberRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMember(c(u, s, r), id, body));
    }
}
