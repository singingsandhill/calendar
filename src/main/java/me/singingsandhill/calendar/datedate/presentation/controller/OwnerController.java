package me.singingsandhill.calendar.datedate.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.presentation.dto.response.ScheduleResponse;

@Controller
public class OwnerController {

    private final OwnerService ownerService;
    private final SeoService seoService;

    public OwnerController(OwnerService ownerService, SeoService seoService) {
        this.ownerService = ownerService;
        this.seoService = seoService;
    }

    @GetMapping("/{ownerId}")
    public String dashboard(@PathVariable String ownerId, Model model) {
        Owner owner = ownerService.getOrCreateOwner(ownerId);
        List<Schedule> schedules = ownerService.getOwnerSchedules(ownerId);

        List<ScheduleResponse> scheduleResponses = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());

        model.addAttribute("ownerId", ownerId);
        model.addAttribute("owner", owner);
        model.addAttribute("schedules", scheduleResponses);
        model.addAttribute("seo", seoService.getDashboardSeo(ownerId));

        return "owner/dashboard";
    }
}
