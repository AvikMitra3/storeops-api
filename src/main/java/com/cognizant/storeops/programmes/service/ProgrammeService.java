package com.cognizant.storeops.programmes.service;

import com.cognizant.storeops.programmes.model.*;
import com.cognizant.storeops.programmes.repository.ProgrammeRepository;
import com.cognizant.storeops.programmes.routes.*;
import com.cognizant.storeops.shared.auth.RequestContext;
import com.cognizant.storeops.shared.error.NotFoundError;
import com.cognizant.storeops.shared.events.*;
import com.cognizant.storeops.staff.service.StaffService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ProgrammeService {
    private final ProgrammeRepository repository;
    private final StaffService staffService;
    private final EventBus eventBus;

    public ProgrammeService(ProgrammeRepository r, StaffService s, EventBus e) {
        repository = r;
        staffService = s;
        eventBus = e;
    }

    public List<Project> list(RequestContext c) {
        return repository.findByStoreId(c.storeId());
    }

    public Project create(RequestContext c, CreateProjectRequest req) {
        Project p = new Project(UUID.randomUUID().toString(), c.storeId(), req.name(), req.description(), c.userId(),
                Instant.now());
        repository.save(p);
        eventBus.emit(new DomainEvent("PROGRAMME_CREATED", Instant.now(), Map.of("programmeId", p.id())));
        return p;
    }

    public ProjectMember addMember(RequestContext c, String id, AddMemberRequest req) {
        Project p = repository.findById(id).filter(x -> x.storeId().equals(c.storeId()))
                .orElseThrow(() -> new NotFoundError("Programme not found: " + id));
        staffService.getById(req.userId());
        ProjectMember m = new ProjectMember(p.id(), req.userId(), req.role());
        return repository.addMember(m);
    }
}
