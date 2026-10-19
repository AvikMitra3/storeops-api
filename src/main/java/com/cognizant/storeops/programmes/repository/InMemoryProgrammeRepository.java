package com.cognizant.storeops.programmes.repository;

import com.cognizant.storeops.programmes.model.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProgrammeRepository implements ProgrammeRepository {
    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final List<ProjectMember> members = new CopyOnWriteArrayList<>();

    public List<Project> findByStoreId(String storeId) {
        return projects.values().stream().filter(x -> x.storeId().equals(storeId)).toList();
    }

    public Optional<Project> findById(String id) {
        return Optional.ofNullable(projects.get(id));
    }

    public Project save(Project p) {
        projects.put(p.id(), p);
        return p;
    }

    public ProjectMember addMember(ProjectMember m) {
        members.add(m);
        return m;
    }
}
