package me.singingsandhill.calendar.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.application.service.ScheduleService;
import me.singingsandhill.calendar.domain.schedule.Schedule;
import me.singingsandhill.calendar.presentation.dto.request.ScheduleCreateRequest;
import me.singingsandhill.calendar.presentation.dto.request.ScheduleUpdateRequest;
import me.singingsandhill.calendar.presentation.dto.response.ScheduleDetailResponse;
import me.singingsandhill.calendar.presentation.dto.response.ScheduleResponse;

@RestController
@RequestMapping("/api/owners/{ownerId}/schedules")
public class ScheduleApiController {

    private final ScheduleService scheduleService;

    public ScheduleApiController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<ScheduleDetailResponse> getSchedule(
            @PathVariable String ownerId,
            @PathVariable int year,
            @PathVariable int month) {
        Schedule schedule = scheduleService.getScheduleByOwnerAndYearMonth(ownerId, year, month);
        return ResponseEntity.ok(ScheduleDetailResponse.from(schedule));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @PathVariable String ownerId,
            @Valid @RequestBody ScheduleCreateRequest request) {
        Schedule schedule = scheduleService.createSchedule(
                ownerId,
                request.year(),
                request.month(),
                request.weeks()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduleResponse.from(schedule));
    }

    @DeleteMapping("/{year}/{month}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable String ownerId,
            @PathVariable int year,
            @PathVariable int month) {
        scheduleService.deleteSchedule(ownerId, year, month);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{year}/{month}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable String ownerId,
            @PathVariable int year,
            @PathVariable int month,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        Schedule schedule = scheduleService.updateSchedule(ownerId, year, month, request.weeks());
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }
}
