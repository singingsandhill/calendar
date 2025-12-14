package me.singingsandhill.calendar.presentation.dto.response;

import java.time.LocalDateTime;

import me.singingsandhill.calendar.domain.owner.Owner;

public record OwnerResponse(
        String ownerId,
        LocalDateTime createdAt,
        int scheduleCount
) {
    public static OwnerResponse from(Owner owner) {
        return new OwnerResponse(
                owner.getOwnerId(),
                owner.getCreatedAt(),
                owner.getScheduleCount()
        );
    }

    public static OwnerResponse from(Owner owner, int scheduleCount) {
        return new OwnerResponse(
                owner.getOwnerId(),
                owner.getCreatedAt(),
                scheduleCount
        );
    }
}
