package me.singingsandhill.calendar.datedate.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletResponse;
import me.singingsandhill.calendar.datedate.application.exception.OwnerNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.application.service.SeoService;
import me.singingsandhill.calendar.datedate.domain.owner.ReservedOwnerIds;
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

    @GetMapping("/{ownerId:[a-z0-9-]{2,20}}")
    public String dashboard(@PathVariable String ownerId, Model model, HttpServletResponse response) {
        if (ReservedOwnerIds.isReserved(ownerId)) {
            throw new OwnerNotFoundException(ownerId);
        }
        if (!ownerService.ownerExists(ownerId)) {
            // ADR datedate/domain/0004: GET 은 owner 를 생성하지 않는다. 미존재 owner 는
            // 동일한 빈 상태 대시보드(일정 만들기 CTA)를 404 로 렌더링 — 소프트 404 제거 +
            // 봇 발 DB row 생성 차단. 생성 경로는 POST /start 와 schedule 생성뿐.
            // sendError() 는 error 페이지로 포워딩되므로 setStatus() 를 사용해야 한다.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        List<Schedule> schedules = ownerService.getOwnerSchedules(ownerId);

        List<ScheduleResponse> scheduleResponses = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());

        model.addAttribute("ownerId", ownerId);
        model.addAttribute("schedules", scheduleResponses);
        model.addAttribute("seo", seoService.getDashboardSeo(ownerId));

        return "owner/dashboard";
    }
}
