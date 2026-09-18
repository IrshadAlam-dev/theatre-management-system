package com.example.theatre.mapper;

import com.example.theatre.dto.TheatreRequest;
import com.example.theatre.dto.TheatreResponse;
import com.example.theatre.entity.Theatre;

public final class TheatreMapper {
    private TheatreMapper() {}
    public static Theatre create(TheatreRequest request) { Theatre value = new Theatre(); update(value, request); return value; }
    public static void update(Theatre value, TheatreRequest request) { value.setName(request.name()); value.setAddress(request.address()); value.setCity(request.city()); }
    public static TheatreResponse toResponse(Theatre value) { return new TheatreResponse(value.getId(), value.getName(), value.getAddress(), value.getCity(), value.getTotalScreens(), value.getCreatedAt()); }
}
