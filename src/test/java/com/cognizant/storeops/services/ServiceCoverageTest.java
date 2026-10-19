package com.cognizant.storeops.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cognizant.storeops.activities.model.*;
import com.cognizant.storeops.activities.repository.ActivityRepository;
import com.cognizant.storeops.activities.routes.*;
import com.cognizant.storeops.activities.service.ActivityService;
import com.cognizant.storeops.alerts.repository.AlertRepository;
import com.cognizant.storeops.alerts.service.AlertService;
import com.cognizant.storeops.programmes.repository.ProgrammeRepository;
import com.cognizant.storeops.programmes.routes.*;
import com.cognizant.storeops.programmes.service.ProgrammeService;
import com.cognizant.storeops.reports.repository.ReportRepository;
import com.cognizant.storeops.reports.service.ReportService;
import com.cognizant.storeops.shared.auth.RequestContext;
import com.cognizant.storeops.shared.error.*;
import com.cognizant.storeops.shared.events.EventBus;
import com.cognizant.storeops.staff.model.*;
import com.cognizant.storeops.staff.repository.StaffRepository;
import com.cognizant.storeops.staff.service.StaffService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ServiceCoverageTest {
    RequestContext c = new RequestContext("u", "s", StaffRole.ASSOCIATE);

    @Test
    void staff() {
        StaffRepository r = mock(StaffRepository.class);
        when(r.findById("u")).thenReturn(Optional.of(new User("u", "s", "N", StaffRole.ASSOCIATE)));
        assertEquals("u", new StaffService(r).getById("u").id());
        assertThrows(NotFoundError.class, () -> new StaffService(r).getById("x"));
    }

    @Test
    void reportsAndAlerts() {
        assertThrows(NotFoundError.class, () -> new ReportService(id -> Optional.empty()).get("x"));
        AlertRepository ar = mock(AlertRepository.class);
        when(ar.findByUserId("u")).thenReturn(List.of());
        assertTrue(new AlertService(ar).forUser("u").isEmpty());
    }

    @Test
    void activityBranches() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = new ActivityService(r, e);
        Task t = new Task("1", "s", null, "t", TaskStatus.TODO, TaskPriority.LOW, TaskCategory.GENERAL, null, "owner",
                Instant.now());
        when(r.findById("1")).thenReturn(Optional.of(t));
        when(r.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        assertThrows(ForbiddenError.class, () -> s.delete(c, "1"));
        assertEquals(TaskStatus.DONE,
                s.update(c, "1", new UpdateTaskRequest(TaskStatus.DONE, null, null, null)).status());
        assertThrows(NotFoundError.class, () -> s.get(c, "x"));
    }

    @Test
    void requestContexts() {
        assertEquals(StaffRole.ASSOCIATE,
                com.cognizant.storeops.shared.auth.RequestContexts.of("u", "s", "ASSOCIATE").role());
        assertThrows(ValidationError.class,
                () -> com.cognizant.storeops.shared.auth.RequestContexts.of("u", "s", "BOGUS"));
    }

    @Test
    void programmes() {
        ProgrammeRepository p = mock(ProgrammeRepository.class);
        StaffService staff = mock(StaffService.class);
        EventBus e = mock(EventBus.class);
        ProgrammeService s = new ProgrammeService(p, staff, e);
        s.create(c, new CreateProjectRequest("n", null));
        assertTrue(s.list(c).isEmpty());
        assertThrows(NotFoundError.class, () -> s.addMember(c, "x",
                new AddMemberRequest("u", com.cognizant.storeops.programmes.model.ProjectRole.ASSOCIATE)));
    }
}
