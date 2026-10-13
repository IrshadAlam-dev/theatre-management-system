package com.example.theatre.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_activity")
public class AdminActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User admin;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "resource", nullable = false, length = 40)
    private String resource;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    protected AdminActivity() { }

    public AdminActivity(User admin, String action, String resource, Long resourceId) {
        this.admin = admin;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
    }

    public Long getId() { return id; }
    public User getAdmin() { return admin; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public Long getResourceId() { return resourceId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
