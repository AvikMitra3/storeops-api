package com.cognizant.storeops.routes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiMockMvcTest {
    @Autowired
    MockMvc mvc;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder headers(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b) {
        return b.header("X-User-Id", "manager-1").header("X-Store-Id", "store-1").header("X-User-Role",
                "STORE_MANAGER");
    }

    @Test
    void activityCrud() throws Exception {
        String body = "{\"title\":\"Restock\",\"priority\":\"HIGH\",\"category\":\"RESTOCKING\",\"assigneeId\":\"associate-1\"}";
        String json = mvc
                .perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        mvc.perform(headers(get("/api/activities"))).andExpect(status().isOk());
        mvc.perform(headers(get("/api/activities/" + id))).andExpect(status().isOk());
        mvc.perform(headers(patch("/api/activities/" + id)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        mvc.perform(headers(delete("/api/activities/" + id))).andExpect(status().isNoContent());
    }

    @Test
    void apiDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/activities/bulk-status']").exists());
        mvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
    }

    @Autowired
    com.cognizant.storeops.shared.events.InMemoryEventBus eventBus;

    @Test
    void bulkStatusEmitsOneAuditEventPerUpdatedTask() throws Exception {
        String body = "{\"title\":\"Audit me\",\"priority\":\"LOW\",\"category\":\"GENERAL\"}";
        String json = mvc
                .perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        mvc.perform(headers(patch("/api/activities/bulk-status")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"updates\":[{\"activityId\":\"" + id + "\",\"status\":\"DONE\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.failed.length()").value(0));
        long audits = eventBus.events().stream()
                .filter(e -> "ACTIVITY_STATUS_CHANGED".equals(e.type()))
                .filter(e -> id.equals(e.payload().get("activityId")))
                .filter(e -> "TODO".equals(e.payload().get("from")) && "DONE".equals(e.payload().get("to"))
                        && "manager-1".equals(e.payload().get("changedBy")))
                .count();
        Assertions.assertEquals(1, audits);
    }

    @Test
    void programmeAndAlerts() throws Exception {
        String json = mvc
                .perform(headers(post("/api/programmes")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Seasonal rollout\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        mvc.perform(headers(get("/api/programmes"))).andExpect(status().isOk());
        mvc.perform(headers(post("/api/programmes/" + id + "/members")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"associate-1\",\"role\":\"ASSOCIATE\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/alerts").header("X-User-Id", "manager-1")).andExpect(status().isOk());
    }

    @Test
    void validationAndNotFound() throws Exception {
        mvc.perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(headers(get("/api/activities/missing"))).andExpect(status().isNotFound());
    }

    @Test
    void bulkStatusHandlesPartialFailure() throws Exception {
        String body = "{\"title\":\"Handover\",\"priority\":\"HIGH\",\"category\":\"RESTOCKING\"}";
        String json = mvc
                .perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
        String bulk = "{\"updates\":[{\"activityId\":\"" + id + "\",\"status\":\"BLOCKED\"},"
                + "{\"activityId\":\"missing\",\"status\":\"DONE\"},"
                + "{\"activityId\":\"" + id + "\",\"status\":\"TODO\"}]}";
        mvc.perform(headers(patch("/api/activities/bulk-status")).contentType(MediaType.APPLICATION_JSON)
                .content(bulk)).andExpect(status().isOk())
                .andExpect(jsonPath("$.updated.length()").value(1))
                .andExpect(jsonPath("$.updated[0].status").value("BLOCKED"))
                .andExpect(jsonPath("$.failed.length()").value(2))
                .andExpect(jsonPath("$.failed[0].code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.failed[1].code").value("VALIDATION_ERROR"));
        mvc.perform(headers(patch("/api/activities/bulk-status")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"updates\":[]}")).andExpect(status().isBadRequest());
    }

    @Autowired
    org.springframework.context.ApplicationContext context;

    @Test
    void createTaskWithDueDate() throws Exception {
        String body = "{\"title\":\"Due\",\"priority\":\"HIGH\",\"category\":\"GENERAL\","
                + "\"dueDate\":\"2030-01-01T10:00:00Z\"}";
        mvc.perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dueDate").value("2030-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.slaBreachNotifiedAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.slaEscalatedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void createTaskRejectsMalformedDueDate() throws Exception {
        String body = "{\"title\":\"Due\",\"priority\":\"HIGH\",\"category\":\"GENERAL\","
                + "\"dueDate\":\"not-a-date\"}";
        mvc.perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Autowired
    com.cognizant.storeops.activities.service.ActivityService activityService;

    private String createOverdue(String assignee, String title) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"priority\":\"HIGH\",\"category\":\"GENERAL\","
                + (assignee == null ? "" : "\"assigneeId\":\"" + assignee + "\",")
                + "\"dueDate\":\"2020-01-01T00:00:00Z\"}";
        String json = mvc.perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder alertsAs(String user) {
        return get("/api/alerts").header("X-User-Id", user);
    }

    private java.util.List<com.fasterxml.jackson.databind.JsonNode> alertsFor(String user, String id)
            throws Exception {
        String json = mvc.perform(alertsAs(user)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        java.util.List<com.fasterxml.jackson.databind.JsonNode> out = new java.util.ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode n : new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(json)) {
            if (n.get("message").asText().contains(id)) {
                out.add(n);
            }
        }
        return out;
    }

    @Test
    void slaBreachNotifiesDepartmentLead() throws Exception {
        String id = createOverdue("lead-1", "Overdue lead task");
        activityService.scanSlaBreaches(java.time.Instant.parse("2020-01-01T00:01:00Z"));
        java.util.List<com.fasterxml.jackson.databind.JsonNode> mine = alertsFor("lead-1", id);
        Assertions.assertEquals(1, mine.size());
        Assertions.assertEquals("SLA_BREACH", mine.get(0).get("type").asText());
        Assertions.assertEquals("IN_APP", mine.get(0).get("channel").asText());
        Assertions.assertEquals("UNREAD", mine.get(0).get("status").asText());
        Assertions.assertTrue(mine.get(0).get("message").asText().contains("Overdue lead task"));
    }

    @Test
    void slaBreachNotificationVisibleOnlyToRecipient() throws Exception {
        String id = createOverdue("lead-1", "Private lead task");
        activityService.scanSlaBreaches(java.time.Instant.parse("2020-01-01T00:01:00Z"));
        Assertions.assertEquals(1, alertsFor("lead-1", id).size());
        Assertions.assertTrue(alertsFor("manager-1", id).isEmpty());
    }

    private static final java.time.Instant DUE = java.time.Instant.parse("2020-01-01T00:00:00Z");

    @Test
    void slaEscalationNotifiesStoreManager() throws Exception {
        String id = createOverdue("lead-1", "Escalate me");
        activityService.scanSlaBreaches(DUE.plusSeconds(60));
        activityService.scanSlaBreaches(DUE.plusSeconds(30 * 60));
        java.util.List<com.fasterxml.jackson.databind.JsonNode> mgr = alertsFor("manager-1", id);
        Assertions.assertEquals(1, mgr.size());
        Assertions.assertEquals("ESCALATION", mgr.get(0).get("type").asText());
        Assertions.assertEquals("IN_APP", mgr.get(0).get("channel").asText());
        Assertions.assertEquals("UNREAD", mgr.get(0).get("status").asText());
        Assertions.assertTrue(mgr.get(0).get("message").asText().contains("Escalate me"));
        java.util.List<com.fasterxml.jackson.databind.JsonNode> lead = alertsFor("lead-1", id);
        Assertions.assertEquals(1, lead.size());
        Assertions.assertEquals("SLA_BREACH", lead.get(0).get("type").asText());
    }

    @Test
    void noEscalationBeforeGraceOrWhenDone() throws Exception {
        String early = createOverdue("lead-1", "Early task");
        activityService.scanSlaBreaches(DUE.plusSeconds(60));
        activityService.scanSlaBreaches(DUE.plusSeconds(29 * 60));
        Assertions.assertTrue(alertsFor("manager-1", early).isEmpty());
        String done = createOverdue("lead-1", "Done task");
        activityService.scanSlaBreaches(DUE.plusSeconds(60));
        mvc.perform(headers(patch("/api/activities/" + done)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        activityService.scanSlaBreaches(DUE.plusSeconds(30 * 60));
        Assertions.assertTrue(alertsFor("manager-1", done).isEmpty());
    }

    @Nested
    @SpringBootTest(properties = "storeops.sla.grace-period=PT5M")
    @AutoConfigureMockMvc
    class ConfiguredGrace {
        @Autowired
        MockMvc graceMvc;

        @Autowired
        com.cognizant.storeops.activities.service.ActivityService graceService;

        @Test
        void escalationHonoursConfiguredGrace() throws Exception {
            String body = "{\"title\":\"Short grace\",\"priority\":\"CRITICAL\",\"category\":\"GENERAL\","
                    + "\"dueDate\":\"2020-01-01T00:00:00Z\"}";
            String json = graceMvc.perform(headers(post("/api/activities")).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asText();
            graceService.scanSlaBreaches(DUE.plusSeconds(60));
            String before = graceMvc.perform(alertsAs("manager-1")).andReturn().getResponse().getContentAsString();
            Assertions.assertFalse(before.contains(id));
            graceService.scanSlaBreaches(DUE.plusSeconds(5 * 60));
            String after = graceMvc.perform(alertsAs("manager-1")).andReturn().getResponse().getContentAsString();
            Assertions.assertTrue(after.contains(id));
        }
    }

    @Test
    void schedulerIsOffWhenScanDisabled() {
        Assertions.assertTrue(context.getBeansOfType(
                com.cognizant.storeops.activities.service.SlaScanScheduler.class).isEmpty());
    }
}
