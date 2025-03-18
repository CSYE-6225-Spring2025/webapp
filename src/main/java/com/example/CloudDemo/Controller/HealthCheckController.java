package com.example.CloudDemo.Controller;


import com.example.CloudDemo.DTO.UploadResponse;
import com.example.CloudDemo.Model.HealthCheck;
import com.example.CloudDemo.Repository.HealthCheckRepository;
import com.example.CloudDemo.Service.AWSs3service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

@RestController
public class HealthCheckController {

    @Autowired()
    private HealthCheckRepository repository;

    @Autowired
    private AWSs3service awSs3service;


    @RequestMapping(path = "/healthz", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Void> healthCheck(HttpServletRequest httpServletRequest) {
        if (!"GET".equalsIgnoreCase(httpServletRequest.getMethod())) {
            return createResponse(HttpStatus.METHOD_NOT_ALLOWED);
        }
        if (httpServletRequest.getContentLength() > 0 || !httpServletRequest.getParameterMap().isEmpty()) {
            return createResponse(HttpStatus.BAD_REQUEST);
        }
        try {
            repository.save(new HealthCheck());
        } catch (Exception e) {
            return createResponse(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return createResponse(HttpStatus.OK);

    }

    private ResponseEntity<Void> createResponse(HttpStatus status) {
        return ResponseEntity.status(status)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("X-Content-Type-Options", "nosniff")
                .header("Pragma", "no-cache")
                .build();
    }


    @RequestMapping(method = RequestMethod.POST, path = "/v1/file")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        UploadResponse response = awSs3service.uploadFile(file);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/v1/file/{id}")
    public ResponseEntity<UploadResponse> getFileById(@PathVariable String id) {
        try {
            UploadResponse response = awSs3service.getFileById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/v1/file/{id}")
    public ResponseEntity<String> deleteFileById(@PathVariable String id) {
        try {
            awSs3service.deleteFileById(id);
            return ResponseEntity.ok("File deleted successfully: " + id);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


}

