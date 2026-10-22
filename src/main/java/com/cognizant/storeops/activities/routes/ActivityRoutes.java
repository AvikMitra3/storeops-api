package com.cognizant.storeops.activities.routes;

import com.cognizant.storeops.activities.model.*;
import com.cognizant.storeops.activities.service.ActivityService;
import com.cognizant.storeops.shared.auth.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
public class ActivityRoutes {
    private final ActivityService service;

    public ActivityRoutes(ActivityService service) {
        this.service = service;
    }

    private RequestContext context(String u, String s, String r) {
        return RequestContexts.of(u, s, r);
    }

    @GetMapping
    public List<Task> list(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @RequestParam(required = false) String programme,
            @RequestParam(required = false) TaskStatus status) {
        return service.list(context(u, s, r), programme, status);
    }

    @PostMapping
    public ResponseEntity<Task> create(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @Valid @RequestBody CreateTaskRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(context(u, s, r), body));
    }

    @PatchMapping("/bulk-status")
    public BulkStatusResult bulkStatus(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @Valid @RequestBody BulkStatusRequest body) {
        return service.bulkUpdateStatus(context(u, s, r), body);
    }

    @GetMapping("/{id}")
    public Task get(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @PathVariable String id) {
        return service.get(context(u, s, r), id);
    }

    @PatchMapping("/{id}")
    public Task update(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @PathVariable String id, @RequestBody UpdateTaskRequest body) {
        return service.update(context(u, s, r), id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") String u, @RequestHeader("X-Store-Id") String s,
            @RequestHeader("X-User-Role") String r, @PathVariable String id) {
        service.delete(context(u, s, r), id);
        return ResponseEntity.noContent().build();
    }
}
