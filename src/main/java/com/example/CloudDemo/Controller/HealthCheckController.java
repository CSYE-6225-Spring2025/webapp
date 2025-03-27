package com.example.CloudDemo.Controller;


import com.example.CloudDemo.DTO.UploadResponse;
import com.example.CloudDemo.Model.HealthCheck;
import com.example.CloudDemo.Repository.HealthCheckRepository;
import com.example.CloudDemo.Service.AWSs3service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.ResponseEntity.status;

@RestController
public class HealthCheckController {

    @Autowired()
    private HealthCheckRepository repository;

    @Autowired
    private AWSs3service awSs3service;

    @Autowired
    private MeterRegistry meterRegistry;

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);



    @RequestMapping(path = "/healthz", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> healthCheck(HttpServletRequest httpServletRequest) {
        if (!"GET".equalsIgnoreCase(httpServletRequest.getMethod())) {
            logger.info("Received BAD REQUEST for /healthz");
            return createResponse(HttpStatus.METHOD_NOT_ALLOWED);
        }
        if (httpServletRequest.getContentLength() > 0 || !httpServletRequest.getParameterMap().isEmpty()) {
            logger.info("Received BAD REQUEST for /healthz");
            return createResponse(HttpStatus.BAD_REQUEST);
        }
        Timer.Sample dbSample = Timer.start(meterRegistry);
        try {
            logger.info("Received GET request to /healthz");
            meterRegistry.counter("api.calls", "endpoint", "/healthz", "method", "GET").increment();
            repository.save(new HealthCheck());
        } catch (Exception e) {
            logger.error("Error while saving healthCheck to DB", e);
            return createResponse(HttpStatus.SERVICE_UNAVAILABLE);
        }finally {
            dbSample.stop(meterRegistry.timer("db.duration", "operation", "saveHealthCheck"));
        }
        return createResponse(HttpStatus.OK);

    }

    private ResponseEntity<Void> createResponse(HttpStatus status) {
        return status(status)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("X-Content-Type-Options", "nosniff")
                .header("Pragma", "no-cache")
                .build();
    }


    @RequestMapping(method = RequestMethod.POST, path = "/v1/file")
    public ResponseEntity<?> uploadFile(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        logger.info("Received POST request to /v1/file");
        if (file == null || file.isEmpty()) {
            logger.info("Received BAD REQUEST for /v1/file");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }
        meterRegistry.counter("api.calls", "endpoint", "/v1/file", "method", "POST").increment();
        Timer.Sample apiSample = Timer.start(meterRegistry);
        try {
            UploadResponse response = awSs3service.uploadFile(file);
            logger.info("Successfully uploaded file with name: {}", file.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Exception while uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
        finally {
            apiSample.stop(meterRegistry.timer("api.duration", "endpoint", "/v1/file", "method", "POST"));
        }
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.DELETE, RequestMethod.HEAD,
            RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.PUT},
            path = "/v1/file")
    public ResponseEntity<String> returnError() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("");
    }

    @RequestMapping(method = RequestMethod.GET, path = "/v1/file/{id}")
    public ResponseEntity<UploadResponse> getFileById(@PathVariable String id) {
        logger.info("Received GET request to /v1/file");
        meterRegistry.counter("api.calls", "endpoint", "/v1/file/{id}", "method", "GET").increment();
        Timer.Sample apiSample = Timer.start(meterRegistry);
        try {
            UploadResponse response = awSs3service.getFileById(id);
            logger.info("Successfully Got response: {}", response.getFile_name());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Exception while Getting file", e);
            return ResponseEntity.notFound().build();
        }finally {
            apiSample.stop(meterRegistry.timer("api.duration", "endpoint", "/v1/file/{id}", "method", "GET"));
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/v1/file/{id}")
    public ResponseEntity<Void> deleteFileById(@PathVariable String id) {
        logger.info("Received DELETE request to /v1/file");
        meterRegistry.counter("api.calls", "endpoint", "/v1/file/{id}", "method", "DELETE").increment();
        Timer.Sample apiSample = Timer.start(meterRegistry);
        try {
            awSs3service.deleteFileById(id);
            logger.info("Successfully DELETED file ");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RuntimeException e) {
            logger.error("Exception in deleteFileById", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }finally {
            apiSample.stop(meterRegistry.timer("api.duration", "endpoint", "/v1/file/{id}", "method", "DELETE"));
        }
    }

    @RequestMapping(method = {RequestMethod.HEAD,
            RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.PUT}, path = "/v1/file/{id}")
    public ResponseEntity<String> returnErrorForFile() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("");
    }


}

