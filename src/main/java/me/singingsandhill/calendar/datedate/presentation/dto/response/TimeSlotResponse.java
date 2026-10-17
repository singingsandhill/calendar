package me.singingsandhill.calendar.datedate.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;

public record TimeSlotResponse(
        Long id,
        int dayIndex,
        int startMinute,
        int endMinute,
        List<String> voters,
        int voteCount,
        LocalDateTime createdAt
) {
    public static TimeSlotResponse from(TimeSlot timeSlot) {
        return new TimeSlotResponse(
                timeSlot.getId(),
                timeSlot.getDayIndex(),
                timeSlot.getStartMinute(),
                timeSlot.getEndMinute(),
                timeSlot.getVoters(),
                timeSlot.getVoteCount(),
                timeSlot.getCreatedAt()
        );
    }
}
