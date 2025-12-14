package me.singingsandhill.calendar.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import me.singingsandhill.calendar.domain.participant.Participant;

public record ParticipantResponse(
        Long id,
        String name,
        String color,
        List<Integer> selections,
        LocalDateTime updatedAt
) {
    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getName(),
                participant.getColorHex(),
                participant.getSelections(),
                participant.getUpdatedAt()
        );
    }
}
