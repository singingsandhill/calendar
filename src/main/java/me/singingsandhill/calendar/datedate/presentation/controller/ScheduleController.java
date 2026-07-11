package me.singingsandhill.calendar.datedate.presentation.controller;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import me.singingsandhill.calendar.common.presentation.LocaleLinks;
import me.singingsandhill.calendar.datedate.application.service.LocationService;
import me.singingsandhill.calendar.datedate.application.service.MenuService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.location.Location;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.presentation.dto.response.ScheduleDetailResponse;

@Controller
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final LocationService locationService;
    private final MenuService menuService;
    private final SeoService seoService;
    private final LocaleLinks localeLinks;

    public ScheduleController(ScheduleService scheduleService,
                               LocationService locationService, MenuService menuService,
                               SeoService seoService,
                               LocaleLinks localeLinks) {
        this.scheduleService = scheduleService;
        this.locationService = locationService;
        this.menuService = menuService;
        this.seoService = seoService;
        this.localeLinks = localeLinks;
    }

    @GetMapping("/{ownerId}/{year:\\d{4}}/{month:\\d{1,2}}")
    public String viewSchedule(
            @PathVariable String ownerId,
            @PathVariable int year,
            @PathVariable int month,
            Locale locale,
            Model model) {

        if (year < 2024 || year > 2100 || month < 1 || month > 12) {
            return localeLinks.redirect("/");
        }

        Optional<Schedule> scheduleOpt = scheduleService.findScheduleByOwnerAndYearMonth(ownerId, year, month);

        model.addAttribute("ownerId", ownerId);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("yearMonthLabel", formatYearMonth(year, month, locale));
        model.addAttribute("seo", seoService.getScheduleSeo(ownerId, year, month));

        if (scheduleOpt.isEmpty()) {
            model.addAttribute("needsCreation", true);
            return "schedule/create";
        }

        Schedule schedule = scheduleOpt.get();
        List<Location> locations = locationService.getLocationsByScheduleId(schedule.getId());
        List<Menu> menus = menuService.getMenusByScheduleId(schedule.getId());
        model.addAttribute("schedule", ScheduleDetailResponse.from(schedule, locations, menus));

        return "schedule/view";
    }

    private String formatYearMonth(int year, int month, Locale locale) {
        if ("en".equals(locale.getLanguage())) {
            return YearMonth.of(year, month)
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        }
        return year + "년 " + month + "월";
    }
}
