package me.singingsandhill.calendar.datedate.application.dto;

import java.util.List;

/** 연간 recap 집계 결과. topWeekday 는 DayOfWeek.name() (없으면 null). */
public record RecapDto(
        int year,
        String nickname,
        int schedulesCreated,
        int totalParticipants,
        int participationCount,
        int daysSelected,
        String topWeekday,
        Integer busiestMonth,
        List<String> topLocations,
        List<String> topMenus,
        List<String> topCompanions,
        boolean empty
) {
}
