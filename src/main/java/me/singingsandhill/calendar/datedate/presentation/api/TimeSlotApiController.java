package me.singingsandhill.calendar.datedate.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.datedate.application.service.TimeSlotService;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;
import me.singingsandhill.calendar.datedate.presentation.dto.request.TimeSlotCreateRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.request.VoteRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.response.TimeSlotResponse;

@RestController
@RequestMapping("/api")
public class TimeSlotApiController {

    private final TimeSlotService timeSlotService;

    public TimeSlotApiController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @PostMapping("/schedules/{scheduleId}/time-slots")
    public ResponseEntity<TimeSlotResponse> addTimeSlot(
            @PathVariable Long scheduleId,
            @Valid @RequestBody TimeSlotCreateRequest request) {
        TimeSlot timeSlot = timeSlotService.addTimeSlot(
                scheduleId, request.dayIndex(), request.startMinute(), request.endMinute());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TimeSlotResponse.from(timeSlot));
    }

    @PostMapping("/time-slots/{timeSlotId}/votes")
    public ResponseEntity<TimeSlotResponse> vote(
            @PathVariable Long timeSlotId,
            @Valid @RequestBody VoteRequest request) {
        TimeSlot timeSlot = timeSlotService.vote(timeSlotId, request.voterName());
        return ResponseEntity.ok(TimeSlotResponse.from(timeSlot));
    }

    @DeleteMapping("/time-slots/{timeSlotId}/votes/{voterName}")
    public ResponseEntity<TimeSlotResponse> unvote(
            @PathVariable Long timeSlotId,
            @PathVariable String voterName) {
        TimeSlot timeSlot = timeSlotService.unvote(timeSlotId, voterName);
        return ResponseEntity.ok(TimeSlotResponse.from(timeSlot));
    }
}
