package com.cognizant.storeops.programmes.repository;

import com.cognizant.storeops.programmes.model.*;
import java.util.*;

public interface ProgrammeRepository {
    List<Project> findByStoreId(String storeId);

    Optional<Project> findById(String id);

    Project save(Project p);

    ProjectMember addMember(ProjectMember m);
}
