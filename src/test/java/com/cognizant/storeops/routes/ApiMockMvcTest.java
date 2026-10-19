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
}
