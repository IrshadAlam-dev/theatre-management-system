package com.example.theatre.mapper;

import com.example.theatre.dto.SeatRequest;
import com.example.theatre.dto.SeatResponse;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Seat;

public final class SeatMapper {
    private SeatMapper() {}
    public static Seat create(SeatRequest request, Screen screen) { Seat value = new Seat(); update(value, request, screen); return value; }
    public static void update(Seat value, SeatRequest request, Screen screen) { value.setSeatNumber(request.seatNumber()); value.setRowNumber(request.rowNumber()); value.setSeatType(request.seatType()); value.setPrice(request.price()); value.setScreen(screen); }
    public static SeatResponse toResponse(Seat value) { return new SeatResponse(value.getId(), value.getSeatNumber(), value.getRowNumber(), value.getSeatType(), value.getPrice(), value.getScreen().getId()); }
}
