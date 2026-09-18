package com.example.theatre.mapper;

import com.example.theatre.dto.ScreenRequest;
import com.example.theatre.dto.ScreenResponse;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Theatre;

public final class ScreenMapper {
    private ScreenMapper() {}
    public static Screen create(ScreenRequest request, Theatre theatre) { Screen value = new Screen(); update(value, request, theatre); return value; }
    public static void update(Screen value, ScreenRequest request, Theatre theatre) { value.setName(request.name()); value.setCapacity(request.capacity()); value.setTheatre(theatre); }
    public static ScreenResponse toResponse(Screen value) { return new ScreenResponse(value.getId(), value.getName(), value.getCapacity(), value.getTheatre().getId()); }
}
