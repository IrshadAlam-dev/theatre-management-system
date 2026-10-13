package com.example.theatre.service;

import com.example.theatre.dto.AdminActivityResponse;
import com.example.theatre.entity.AdminActivity;
import com.example.theatre.repository.AdminActivityRepository;
import com.example.theatre.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActivityService {
    private final AdminActivityRepository activities;
    private final UserRepository users;

    public AdminActivityService(AdminActivityRepository activities, UserRepository users) {
        this.activities = activities;
        this.users = users;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long adminId, String action, String resource, Long resourceId) {
        users.findById(adminId).ifPresent(admin ->
                activities.save(new AdminActivity(admin, action, resource, resourceId)));
    }

    @Transactional(readOnly = true)
    public Page<AdminActivityResponse> recent(Pageable pageable) {
        return activities.findAllByOrderByOccurredAtDesc(pageable)
                .map(activity -> new AdminActivityResponse(activity.getId(), activity.getAdmin().getId(),
                        activity.getAdmin().getName(), activity.getAction(), activity.getResource(),
                        activity.getResourceId(), activity.getOccurredAt()));
    }
}
