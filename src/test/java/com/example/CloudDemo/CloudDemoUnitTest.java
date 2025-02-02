package com.example.CloudDemo;

import com.example.CloudDemo.Controller.HealthCheckController;
import com.example.CloudDemo.Repository.HealthCheckRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
