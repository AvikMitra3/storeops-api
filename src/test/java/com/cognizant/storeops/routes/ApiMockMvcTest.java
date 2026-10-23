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
}
