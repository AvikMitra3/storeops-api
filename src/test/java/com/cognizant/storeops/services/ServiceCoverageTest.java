package com.cognizant.storeops.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cognizant.storeops.activities.model.*;
import com.cognizant.storeops.activities.repository.ActivityRepository;
import com.cognizant.storeops.activities.repository.InMemoryActivityRepository;
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
import com.cognizant.storeops.shared.events.DomainEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import com.cognizant.storeops.activities.service.SlaScanScheduler;
import org.mockito.ArgumentCaptor;
import java.util.*;
import org.junit.jupiter.api.Test;

class ServiceCoverageTest {
    RequestContext c = new RequestContext("u", "s", StaffRole.ASSOCIATE);

    @Test
    void staffFindByStoreAndRole() {
        StaffService s = new StaffService(new com.cognizant.storeops.staff.repository.InMemoryStaffRepository());
        List<User> r = s.findByStoreAndRole("store-1", StaffRole.STORE_MANAGER);
        assertEquals(1, r.size());
        assertEquals("manager-1", r.get(0).id());
    }

    @Test
    void staffFindByStoreAndRoleEmpty() {
        StaffService s = new StaffService(new com.cognizant.storeops.staff.repository.InMemoryStaffRepository());
        assertTrue(s.findByStoreAndRole("store-2", StaffRole.STORE_MANAGER).isEmpty());
    }

    @Test
    void staffSeedContainsDepartmentLead() {
        User u = new com.cognizant.storeops.staff.repository.InMemoryStaffRepository().findById("lead-1").orElseThrow();
        assertEquals("store-1", u.storeId());
        assertEquals(StaffRole.DEPARTMENT_LEAD, u.role());
        assertEquals("manager-1", new com.cognizant.storeops.staff.repository.InMemoryStaffRepository()
                .findById("manager-1").orElseThrow().id());
    }

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
        assertTrue(new AlertService(ar, mock(StaffService.class)).forUser("u").isEmpty());
    }

    @Test
    void activityBranches() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = new ActivityService(r, e, Duration.ofMinutes(30));
        Task t = new Task("1", "s", null, "t", TaskStatus.TODO, TaskPriority.LOW, TaskCategory.GENERAL, null, "owner",
                Instant.now(), null, null, null);
        when(r.findById("1")).thenReturn(Optional.of(t));
        when(r.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        assertThrows(ForbiddenError.class, () -> s.delete(c, "1"));
        assertEquals(TaskStatus.DONE,
                s.update(c, "1", new UpdateTaskRequest(TaskStatus.DONE, null, null, null, null)).status());
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

    private static final Instant T = Instant.parse("2030-01-01T10:00:00Z");

    private Task sla(String id, TaskStatus st, TaskPriority p, Instant due) {
        return new Task(id, "s", null, "t" + id, st, p, TaskCategory.GENERAL, "lead-1", "o", T, due, null, null);
    }

    private ActivityService slaService(ActivityRepository r, EventBus e) {
        when(r.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
        return new ActivityService(r, e, Duration.parse("PT30M"));
    }

    @Test
    void slaScanEmitsBreachOncePerTask() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = slaService(r, e);
        Task a = sla("a", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, T);
        Task b = sla("b", TaskStatus.TODO, TaskPriority.CRITICAL, T);
        Instant now = T.plusSeconds(60);
        when(r.findSlaCandidates()).thenReturn(List.of(a, b));
        s.scanSlaBreaches(now);
        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(r, times(2)).save(saved.capture());
        assertTrue(saved.getAllValues().stream().allMatch(x -> now.equals(x.slaBreachNotifiedAt())));
        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(e, times(2)).emit(ev.capture());
        assertEquals("SLA_BREACH_DETECTED", ev.getAllValues().get(0).type());
        assertEquals(Map.of("activityId", "a", "storeId", "s", "assigneeId", "lead-1", "title", "ta",
                "priority", "HIGH",
                "dueDate", T.toString()), ev.getAllValues().get(0).payload());
        assertEquals("CRITICAL", ev.getAllValues().get(1).payload().get("priority"));
        when(r.findSlaCandidates()).thenReturn(saved.getAllValues());
        s.scanSlaBreaches(now.plusSeconds(60));
        verify(e, times(2)).emit(any(DomainEvent.class));
    }

    @Test
    void slaScanIgnoresNonQualifyingTasks() {
        InMemoryActivityRepository r = new InMemoryActivityRepository();
        EventBus e = mock(EventBus.class);
        ActivityService s = new ActivityService(r, e, Duration.parse("PT30M"));
        Instant past = T.minusSeconds(3600);
        List<Task> tasks = List.of(sla("m", TaskStatus.TODO, TaskPriority.MEDIUM, past),
                sla("l", TaskStatus.TODO, TaskPriority.LOW, past),
                sla("d", TaskStatus.DONE, TaskPriority.HIGH, past),
                sla("n", TaskStatus.TODO, TaskPriority.CRITICAL, null),
                sla("f", TaskStatus.TODO, TaskPriority.HIGH, T.plusSeconds(600)));
        tasks.forEach(r::save);
        s.scanSlaBreaches(T);
        verify(e, never()).emit(any(DomainEvent.class));
        tasks.forEach(t -> assertEquals(t, r.findById(t.id()).orElseThrow()));
    }

    @Test
    void slaScanEscalatesAfterGraceOnce() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = slaService(r, e);
        Task breached = new Task("a", "s", null, "t", TaskStatus.BLOCKED, TaskPriority.HIGH, TaskCategory.GENERAL,
                null, "o", T, T, T.plusSeconds(60), null);
        when(r.findSlaCandidates()).thenReturn(List.of(breached));
        s.scanSlaBreaches(T.plus(Duration.ofMinutes(29)));
        verify(e, never()).emit(any(DomainEvent.class));
        s.scanSlaBreaches(T.plus(Duration.ofMinutes(30)));
        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(e, times(1)).emit(ev.capture());
        assertEquals("SLA_ESCALATION_DUE", ev.getValue().type());
        assertEquals("", ev.getValue().payload().get("assigneeId"));
        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(r).save(saved.capture());
        assertEquals(T.plus(Duration.ofMinutes(30)), saved.getValue().slaEscalatedAt());
        when(r.findSlaCandidates()).thenReturn(List.of(saved.getValue()));
        s.scanSlaBreaches(T.plus(Duration.ofMinutes(31)));
        verify(e, times(1)).emit(any(DomainEvent.class));
    }

    @Test
    void slaScanBreachAndEscalationInOneScanWhenFarOverdue() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = slaService(r, e);
        when(r.findSlaCandidates()).thenReturn(List.of(sla("a", TaskStatus.TODO, TaskPriority.HIGH, T)));
        s.scanSlaBreaches(T.plus(Duration.ofHours(2)));
        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(e, times(2)).emit(ev.capture());
        assertEquals("SLA_BREACH_DETECTED", ev.getAllValues().get(0).type());
        assertEquals("SLA_ESCALATION_DUE", ev.getAllValues().get(1).type());
    }

    @Test
    void slaScanSkipsEscalationWhenDone() {
        InMemoryActivityRepository r = new InMemoryActivityRepository();
        EventBus e = mock(EventBus.class);
        ActivityService s = new ActivityService(r, e, Duration.parse("PT30M"));
        r.save(sla("a", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, T));
        s.scanSlaBreaches(T.plusSeconds(60));
        verify(e, times(1)).emit(any(DomainEvent.class));
        Task done = s.update(c, "a", new UpdateTaskRequest(TaskStatus.DONE, null, null, null, null));
        assertEquals(T.plusSeconds(60), done.slaBreachNotifiedAt());
        s.scanSlaBreaches(T.plus(Duration.ofMinutes(31)));
        verify(e, times(1)).emit(any(DomainEvent.class));
    }

    @Test
    void schedulerUsesClock() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = slaService(r, e);
        when(r.findSlaCandidates()).thenReturn(List.of(sla("a", TaskStatus.TODO, TaskPriority.HIGH, T)));
        new SlaScanScheduler(s, Clock.fixed(T.plusSeconds(60), ZoneOffset.UTC)).scan();
        verify(e).emit(any(DomainEvent.class));
    }

    private DomainEvent breachEvent(String assigneeId) {
        return new DomainEvent("SLA_BREACH_DETECTED", T, Map.of("activityId", "a1", "storeId", "store-1",
                "assigneeId", assigneeId, "title", "Fix shelf", "priority", "HIGH", "dueDate", T.toString()));
    }

    @Test
    void eventBusDispatchesByType() {
        com.cognizant.storeops.shared.events.InMemoryEventBus bus =
                new com.cognizant.storeops.shared.events.InMemoryEventBus();
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe("X", received::add);
        DomainEvent x = new DomainEvent("X", T, Map.of());
        DomainEvent y = new DomainEvent("Y", T, Map.of());
        bus.emit(x);
        bus.emit(y);
        assertEquals(List.of(x), received);
        assertEquals(List.of(x, y), bus.events());
    }

    @Test
    void eventBusIsolatesSubscriberFailures() {
        com.cognizant.storeops.shared.events.InMemoryEventBus bus =
                new com.cognizant.storeops.shared.events.InMemoryEventBus();
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe("X", ev -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe("X", received::add);
        DomainEvent x = new DomainEvent("X", T, Map.of());
        assertDoesNotThrow(() -> bus.emit(x));
        assertEquals(List.of(x), received);
    }

    @Test
    void breachListenerSkipsNonLeadAssignee() {
        AlertRepository ar = mock(AlertRepository.class);
        StaffService staff = mock(StaffService.class);
        when(staff.getById("associate-1")).thenReturn(new User("associate-1", "store-1", "A", StaffRole.ASSOCIATE));
        when(staff.getById("ghost")).thenThrow(new NotFoundError("missing"));
        com.cognizant.storeops.shared.events.InMemoryEventBus bus =
                new com.cognizant.storeops.shared.events.InMemoryEventBus();
        new com.cognizant.storeops.alerts.service.SlaAlertListener(bus, new AlertService(ar, staff));
        bus.emit(breachEvent("associate-1"));
        bus.emit(breachEvent(""));
        bus.emit(breachEvent("ghost"));
        verify(ar, never()).save(any());
        assertEquals(3, bus.events().size());
    }

    @Test
    void breachListenerNotifiesLead() {
        AlertRepository ar = mock(AlertRepository.class);
        StaffService staff = mock(StaffService.class);
        when(staff.getById("lead-1")).thenReturn(new User("lead-1", "store-1", "L", StaffRole.DEPARTMENT_LEAD));
        new AlertService(ar, staff).notifySlaBreach(breachEvent("lead-1"));
        ArgumentCaptor<com.cognizant.storeops.alerts.model.Notification> n =
                ArgumentCaptor.forClass(com.cognizant.storeops.alerts.model.Notification.class);
        verify(ar).save(n.capture());
        assertEquals("lead-1", n.getValue().userId());
        assertEquals(com.cognizant.storeops.alerts.model.AlertType.SLA_BREACH, n.getValue().type());
        assertTrue(n.getValue().message().contains("Fix shelf") && n.getValue().message().contains("a1"));
    }

    private DomainEvent escalationEvent(String storeId, String assigneeId) {
        return new DomainEvent("SLA_ESCALATION_DUE", T, Map.of("activityId", "a1", "storeId", storeId,
                "assigneeId", assigneeId, "title", "Fix shelf", "priority", "CRITICAL", "dueDate", T.toString()));
    }

    @Test
    void escalationTargetsOnlySameStoreManager() {
        AlertRepository ar = mock(AlertRepository.class);
        StaffService staff = mock(StaffService.class);
        when(staff.findByStoreAndRole("store-1", StaffRole.STORE_MANAGER))
                .thenReturn(List.of(new User("manager-1", "store-1", "M", StaffRole.STORE_MANAGER)));
        when(staff.findByStoreAndRole("store-2", StaffRole.STORE_MANAGER)).thenReturn(List.of());
        new AlertService(ar, staff).notifyEscalation(escalationEvent("store-2", ""));
        verify(ar, never()).save(any());
        verify(staff, never()).findByStoreAndRole("store-1", StaffRole.STORE_MANAGER);
    }

    @Test
    void escalationWithoutManagerIsSkipped() {
        AlertRepository ar = mock(AlertRepository.class);
        StaffService staff = mock(StaffService.class);
        when(staff.findByStoreAndRole("store-9", StaffRole.STORE_MANAGER)).thenReturn(List.of());
        assertDoesNotThrow(() -> new AlertService(ar, staff).notifyEscalation(escalationEvent("store-9", "x")));
        verify(ar, never()).save(any());
    }

    @Test
    void unassignedTaskStillEscalates() {
        AlertRepository ar = mock(AlertRepository.class);
        StaffService staff = mock(StaffService.class);
        when(staff.findByStoreAndRole("store-1", StaffRole.STORE_MANAGER))
                .thenReturn(List.of(new User("manager-1", "store-1", "M", StaffRole.STORE_MANAGER)));
        com.cognizant.storeops.shared.events.InMemoryEventBus bus =
                new com.cognizant.storeops.shared.events.InMemoryEventBus();
        new com.cognizant.storeops.alerts.service.SlaAlertListener(bus, new AlertService(ar, staff));
        bus.emit(breachEvent(""));
        bus.emit(escalationEvent("store-1", ""));
        ArgumentCaptor<com.cognizant.storeops.alerts.model.Notification> n =
                ArgumentCaptor.forClass(com.cognizant.storeops.alerts.model.Notification.class);
        verify(ar).save(n.capture());
        assertEquals("manager-1", n.getValue().userId());
        assertEquals(com.cognizant.storeops.alerts.model.AlertType.ESCALATION, n.getValue().type());
        assertTrue(n.getValue().message().contains("Fix shelf") && n.getValue().message().contains("a1"));
        verify(staff, never()).getById(any());
    }

    @Test
    void updateKeepsAndChangesDueDate() {
        ActivityRepository r = mock(ActivityRepository.class);
        EventBus e = mock(EventBus.class);
        ActivityService s = slaService(r, e);
        Task t = sla("a", TaskStatus.TODO, TaskPriority.HIGH, T);
        when(r.findById("a")).thenReturn(Optional.of(t));
        RequestContext sc = new RequestContext("u", "s", StaffRole.ASSOCIATE);
        assertEquals(T, s.update(sc, "a", new UpdateTaskRequest(null, null, null, null, null)).dueDate());
        assertEquals(T.plusSeconds(5),
                s.update(sc, "a", new UpdateTaskRequest(null, null, null, null, T.plusSeconds(5))).dueDate());
    }
}
