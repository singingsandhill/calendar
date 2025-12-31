package me.singingsandhill.calendar.datedate.presentation.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.presentation.dto.request.OwnerCreateRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.response.OwnerResponse;
import me.singingsandhill.calendar.datedate.presentation.dto.response.ScheduleResponse;

@RestController
@RequestMapping("/api/owners")
public class OwnerApiController {

    private final OwnerService ownerService;

    public OwnerApiController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponse> getOwner(@PathVariable String ownerId) {
        Owner owner = ownerService.getOwner(ownerId);
        if (owner == null) {
            return ResponseEntity.notFound().build();
        }
        List<Schedule> schedules = ownerService.getOwnerSchedules(ownerId);
        return ResponseEntity.ok(OwnerResponse.from(owner, schedules.size()));
    }

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerCreateRequest request) {
        Owner owner = ownerService.getOrCreateOwner(request.ownerId());
        List<Schedule> schedules = ownerService.getOwnerSchedules(request.ownerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OwnerResponse.from(owner, schedules.size()));
    }

    @GetMapping("/{ownerId}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getOwnerSchedules(@PathVariable String ownerId) {
        List<Schedule> schedules = ownerService.getOwnerSchedules(ownerId);
        List<ScheduleResponse> responses = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
