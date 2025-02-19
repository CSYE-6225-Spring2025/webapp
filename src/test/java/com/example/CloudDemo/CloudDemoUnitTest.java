package com.example.CloudDemo;

import com.example.CloudDemo.Repository.HealthCheckRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
public class CloudDemoUnitTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private HealthCheckRepository repository;

    @BeforeEach
    void createTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS health_check (" +
                "check_id BIGINT NOT NULL AUTO_INCREMENT, " +
                "datetime TIMESTAMP NOT NULL, " +
                "PRIMARY KEY (check_id)" +
                ")");
    }

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
        mockMvc.perform(get("/healthz").param("key", "value"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testHealthCheck_Throw503WhenDatabaseActionFails() throws Exception {
        jdbcTemplate.execute("DROP TABLE IF EXISTS health_check");
        mockMvc.perform(get("/healthz"))
                .andExpect(status().is5xxServerError());
    }
}
