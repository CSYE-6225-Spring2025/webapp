package com.example.CloudDemo.Controller;


import com.example.CloudDemo.Model.HealthCheck;
import com.example.CloudDemo.Repository.HealthCheckRepository;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Autowired()
    private HealthCheckRepository repository;

    @GetMapping(path = "/healths")
    public ResponseEntity<Void> healthCheck() {
            try {
                repository.save(new HealthCheck());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Cache-Control", "no-cache, no-store, must-revalidate").build();
            }
        return ResponseEntity.status(HttpStatus.OK).header("Cache-Control", "no-cache, no-store, must-revalidate").build();

    }
}
