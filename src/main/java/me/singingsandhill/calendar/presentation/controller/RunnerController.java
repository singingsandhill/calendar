package me.singingsandhill.calendar.presentation.controller;

import me.singingsandhill.calendar.application.service.AttendanceService;
import me.singingsandhill.calendar.application.service.RunService;
import me.singingsandhill.calendar.domain.runner.Attendance;
import me.singingsandhill.calendar.domain.runner.AttendanceRankingDto;
import me.singingsandhill.calendar.domain.runner.DistanceRankingDto;
import me.singingsandhill.calendar.domain.runner.MemberAttendanceStatsDto;
import me.singingsandhill.calendar.domain.runner.Run;
import me.singingsandhill.calendar.presentation.dto.response.AttendanceResponse;
import me.singingsandhill.calendar.presentation.dto.response.AttendanceWithRunResponse;
import me.singingsandhill.calendar.presentation.dto.response.MemberStatsResponse;
import me.singingsandhill.calendar.presentation.dto.response.RunResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/runners")
public class RunnerController {

    private final RunService runService;
    private final AttendanceService attendanceService;

    public RunnerController(RunService runService, AttendanceService attendanceService) {
        this.runService = runService;
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public String home(Model model) {
        List<AttendanceRankingDto> attendanceRankings = attendanceService.getTop10ByAttendanceCount();
        List<DistanceRankingDto> distanceRankings = attendanceService.getTop10ByTotalDistance();

        model.addAttribute("attendanceRankings", attendanceRankings);
        model.addAttribute("distanceRankings", distanceRankings);

        return "runners/home";
    }

    @GetMapping("/runs")
    public String runList(Model model) {
        List<Run> runs = runService.getAllRuns();
        List<RunResponse> runResponses = runs.stream()
                .map(RunResponse::from)
                .collect(Collectors.toList());

        model.addAttribute("runs", runResponses);
        return "runners/run-list";
    }

    @GetMapping("/runs/{id}")
    public String runDetail(@PathVariable Long id, Model model) {
        Run run = runService.getRunById(id);
        List<Attendance> attendances = attendanceService.getAttendancesByRunId(id);

        model.addAttribute("run", RunResponse.from(run));
        model.addAttribute("attendances", attendances.stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList()));

        return "runners/run-detail";
    }

    @GetMapping("/members")
    public String memberList(Model model) {
        List<MemberAttendanceStatsDto> memberStats = attendanceService.getAllMemberStats();
        List<MemberStatsResponse> memberResponses = memberStats.stream()
                .map(MemberStatsResponse::from)
                .collect(Collectors.toList());

        model.addAttribute("members", memberResponses);
        return "runners/member-list";
    }

    @GetMapping("/members/{name}")
    public String memberDetail(@PathVariable String name, Model model) {
        List<Attendance> attendances = attendanceService.getAttendancesByParticipantName(name);

        List<AttendanceWithRunResponse> attendanceResponses = attendances.stream()
                .map(attendance -> {
                    Run run = runService.getRunById(attendance.getRunId());
                    return AttendanceWithRunResponse.from(attendance, run);
                })
                .collect(Collectors.toList());

        long regularCount = attendanceResponses.stream()
                .filter(a -> "REGULAR".equals(a.category()))
                .count();
        long lightningCount = attendanceResponses.stream()
                .filter(a -> "LIGHTNING".equals(a.category()))
                .count();

        model.addAttribute("memberName", name);
        model.addAttribute("attendances", attendanceResponses);
        model.addAttribute("regularCount", regularCount);
        model.addAttribute("lightningCount", lightningCount);
        model.addAttribute("totalCount", attendanceResponses.size());

        return "runners/member-detail";
    }
}
