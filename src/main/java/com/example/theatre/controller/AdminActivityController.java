package com.example.theatre.controller;

import com.example.theatre.dto.AdminActivityResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.AdminActivityService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/activity")
public class AdminActivityController {
    private final AdminActivityService service;

    public AdminActivityController(AdminActivityService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminActivityResponse> recent(@PageableDefault(size = 30) Pageable pageable) {
        return PageResponse.from(service.recent(pageable));
    }
}
