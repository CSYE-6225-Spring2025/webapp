package com.example.CloudDemo;

import com.example.CloudDemo.Controller.HealthCheckController;
import com.example.CloudDemo.Model.HealthCheck;
import com.example.CloudDemo.Repository.HealthCheckRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthCheckController.class)
public class CloudDemoUnitTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthCheckRepository repository;

    @Test
    void testHealthCheck_Successful() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthCheck_Throw404ForWrongEndPoint() throws Exception {
        mockMvc.perform(get("/healths"))
                .andExpect(status().is4xxClientError());
    }
    @Test
    void testHealthCheck_Throw400WhenPayloadIsIncluded() throws Exception {
        mockMvc.perform(get("/healthz").contentType(MediaType.APPLICATION_JSON).content("{\"key\":\"value\"}"))
                .andExpect(status().is4xxClientError());
    }
    @Test
    void testHealthCheck_Throw405WhenHTTPMethodIsWrong() throws Exception {
        mockMvc.perform(post("/healthz"))
                .andExpect(status().is4xxClientError());
    }
    @Test
    void testHealthCheck_Throw400WhenQueryParamIncluded() throws Exception {
        mockMvc.perform(get("/healthz").param("key","value"))
                .andExpect(status().is4xxClientError());
    }
    @Test
    void testHealthCheck_Throw503WhenDatabaseActionFails() throws Exception {
        doThrow(new RuntimeException()).when(repository).save(any(HealthCheck.class));
        mockMvc.perform(get("/healthz"))
                .andExpect(status().is5xxServerError());
    }
}
