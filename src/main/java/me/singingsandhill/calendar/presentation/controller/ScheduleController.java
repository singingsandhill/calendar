package me.singingsandhill.calendar.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import me.singingsandhill.calendar.application.service.OwnerService;
import me.singingsandhill.calendar.application.service.ScheduleService;
import me.singingsandhill.calendar.domain.schedule.Schedule;
import me.singingsandhill.calendar.presentation.dto.response.ScheduleDetailResponse;

@Controller
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final OwnerService ownerService;

    public ScheduleController(ScheduleService scheduleService, OwnerService ownerService) {
        this.scheduleService = scheduleService;
        this.ownerService = ownerService;
    }

    @GetMapping("/{ownerId}/{year}/{month}")
    public String viewSchedule(
            @PathVariable String ownerId,
            @PathVariable int year,
            @PathVariable int month,
            Model model) {

        ownerService.getOrCreateOwner(ownerId);

        Schedule schedule = scheduleService.findScheduleByOwnerAndYearMonth(ownerId, year, month);

        if (schedule == null) {
            schedule = scheduleService.createSchedule(ownerId, year, month, null);
        }

        ScheduleDetailResponse response = ScheduleDetailResponse.from(schedule);

        model.addAttribute("ownerId", ownerId);
        model.addAttribute("schedule", response);
        model.addAttribute("year", year);
        model.addAttribute("month", month);

        return "schedule/view";
    }
}
