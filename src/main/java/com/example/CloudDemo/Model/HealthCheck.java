package com.example.CloudDemo.Model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "health_check")
public class HealthCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkId;

    @Column(nullable = false)
    private Instant datetime;

    public HealthCheck() {
        this.datetime = Instant.now();
    }

    public Long getCheckId() {
        return checkId;
    }

    public Instant getDatetime() {
        return datetime;
    }
}
