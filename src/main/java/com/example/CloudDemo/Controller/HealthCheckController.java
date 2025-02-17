package com.example.CloudDemo.Controller;


import com.example.CloudDemo.Model.HealthCheck;
import com.example.CloudDemo.Repository.HealthCheckRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Autowired()
    private HealthCheckRepository repository;

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
}

