package me.singingsandhill.calendar.datedate.presentation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import me.singingsandhill.calendar.datedate.application.service.LocationService;
import me.singingsandhill.calendar.datedate.application.service.MenuService;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.location.Location;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.presentation.dto.response.ScheduleDetailResponse;

@Controller
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final OwnerService ownerService;
    private final LocationService locationService;
    private final MenuService menuService;
    private final SeoService seoService;

    public ScheduleController(ScheduleService scheduleService, OwnerService ownerService,
                               LocationService locationService, MenuService menuService,
                               SeoService seoService) {
        this.scheduleService = scheduleService;
        this.ownerService = ownerService;
        this.locationService = locationService;
        this.menuService = menuService;
        this.seoService = seoService;
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

        List<Location> locations = locationService.getLocationsByScheduleId(schedule.getId());
        List<Menu> menus = menuService.getMenusByScheduleId(schedule.getId());
        ScheduleDetailResponse response = ScheduleDetailResponse.from(schedule, locations, menus);

        model.addAttribute("ownerId", ownerId);
        model.addAttribute("schedule", response);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("seo", seoService.getScheduleSeo(ownerId, year, month));

        return "schedule/view";
    }
}
