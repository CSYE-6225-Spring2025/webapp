package com.example.CloudDemo.Model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "health_check")
public class HealthCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkId;

    @Column(nullable = false)
    private LocalDateTime datetime;

    public HealthCheck() {
        this.datetime = LocalDateTime.now();
    }

    public Long getCheckId() {
        return checkId;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }
}
