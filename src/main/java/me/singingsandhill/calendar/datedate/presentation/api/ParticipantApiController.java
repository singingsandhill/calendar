package me.singingsandhill.calendar.datedate.presentation.api;

import java.util.List;
import java.util.stream.Collectors;

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
import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.presentation.dto.request.ParticipantCreateRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.request.SelectionUpdateRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.response.ParticipantResponse;

@RestController
@RequestMapping("/api")
public class ParticipantApiController {

    private final ParticipantService participantService;

    public ParticipantApiController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/schedules/{scheduleId}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable Long scheduleId) {
        List<Participant> participants = participantService.getParticipantsByScheduleId(scheduleId);
        List<ParticipantResponse> responses = participants.stream()
                .map(ParticipantResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/schedules/{scheduleId}/participants")
    public ResponseEntity<ParticipantResponse> addParticipant(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ParticipantCreateRequest request) {
        Participant participant = participantService.addParticipant(scheduleId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ParticipantResponse.from(participant));
    }

    @DeleteMapping("/participants/{participantId}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable Long participantId) {
        participantService.deleteParticipant(participantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/participants/{participantId}/selections")
    public ResponseEntity<ParticipantResponse> updateSelections(
            @PathVariable Long participantId,
            @Valid @RequestBody SelectionUpdateRequest request) {
        Participant participant = participantService.updateSelections(participantId, request.selections());
        return ResponseEntity.ok(ParticipantResponse.from(participant));
    }
}
