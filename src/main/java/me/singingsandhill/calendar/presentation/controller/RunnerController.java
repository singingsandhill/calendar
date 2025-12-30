package me.singingsandhill.calendar.presentation.controller;

import me.singingsandhill.calendar.application.service.AttendanceService;
import me.singingsandhill.calendar.application.service.RunService;
import me.singingsandhill.calendar.domain.runner.Attendance;
import me.singingsandhill.calendar.domain.runner.AttendanceRankingDto;
import me.singingsandhill.calendar.domain.runner.DistanceRankingDto;
import me.singingsandhill.calendar.domain.runner.MemberAttendanceStatsDto;
import me.singingsandhill.calendar.domain.runner.Run;
import me.singingsandhill.calendar.presentation.dto.SeoMetadata;
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

    private static final String BASE_URL = "https://datedate.me";
    private static final String OG_IMAGE = "https://datedate.me/image/crew_logo.png";
    private static final String KEYWORDS = "러닝 크루, 러닝, 달리기, 출석체크, 97 runners, 러닝 모임";

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
        model.addAttribute("seo", SeoMetadata.builder()
                .title("97 Runners - 함께 달리는 즐거움")
                .description("97 Runners 러닝 크루 - 출석 체크, 거리 기록, 랭킹 확인. 함께 달리며 건강한 라이프스타일을 만들어갑니다.")
                .keywords(KEYWORDS)
                .canonical(BASE_URL + "/runners")
                .ogImage(OG_IMAGE)
                .build());

        return "runners/home";
    }

    @GetMapping("/runs")
    public String runList(Model model) {
        List<Run> runs = runService.getAllRuns();
        List<RunResponse> runResponses = runs.stream()
                .map(RunResponse::from)
                .collect(Collectors.toList());

        model.addAttribute("runs", runResponses);
        model.addAttribute("seo", SeoMetadata.builder()
                .title("런 목록 - 97 Runners")
                .description("97 Runners 러닝 크루의 정규런, 번개런 일정을 확인하고 출석 체크하세요.")
                .keywords(KEYWORDS)
                .canonical(BASE_URL + "/runners/runs")
                .ogImage(OG_IMAGE)
                .build());
        return "runners/run-list";
    }

    @GetMapping("/runs/{id}")
    public String runDetail(@PathVariable Long id, Model model) {
        Run run = runService.getRunById(id);
        List<Attendance> attendances = attendanceService.getAttendancesByRunId(id);
        RunResponse runResponse = RunResponse.from(run);

        model.addAttribute("run", runResponse);
        model.addAttribute("attendances", attendances.stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList()));
        model.addAttribute("seo", SeoMetadata.builder()
                .title(runResponse.formattedDate() + " " + runResponse.categoryDisplayName() + " - 97 Runners")
                .description("97 Runners " + runResponse.formattedDate() + " " + runResponse.categoryDisplayName() + " - " + runResponse.location() + "에서 함께 달려요!")
                .keywords(KEYWORDS)
                .canonical(BASE_URL + "/runners/runs/" + id)
                .ogImage(OG_IMAGE)
                .build());

        return "runners/run-detail";
    }

    @GetMapping("/members")
    public String memberList(Model model) {
        List<MemberAttendanceStatsDto> memberStats = attendanceService.getAllMemberStats();
        List<MemberStatsResponse> memberResponses = memberStats.stream()
                .map(MemberStatsResponse::from)
                .collect(Collectors.toList());

        model.addAttribute("members", memberResponses);
        model.addAttribute("seo", SeoMetadata.builder()
                .title("출석 현황 - 97 Runners")
                .description("97 Runners 러닝 크루 멤버들의 출석 현황과 누적 거리를 확인하세요.")
                .keywords(KEYWORDS)
                .canonical(BASE_URL + "/runners/members")
                .ogImage(OG_IMAGE)
                .build());
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
        model.addAttribute("seo", SeoMetadata.builder()
                .title(name + " - 97 Runners")
                .description("97 Runners 멤버 " + name + "님의 출석 기록과 누적 거리를 확인하세요.")
                .keywords(KEYWORDS)
                .canonical(BASE_URL + "/runners/members/" + name)
                .ogImage(OG_IMAGE)
                .build());

        return "runners/member-detail";
    }

    @GetMapping("/announce")
    public String announcePage(Model model) {
        model.addAttribute("seo", SeoMetadata.builder()
                .title("공지 이미지 생성기 - 97 Runners")
                .description("97 Runners 러닝 크루 공지 이미지를 쉽게 만들어보세요. 날짜, 시간, 장소를 입력하면 공유 가능한 이미지가 생성됩니다.")
                .keywords(KEYWORDS)
                .canonical(BASE_URL + "/runners/announce")
                .ogImage(OG_IMAGE)
                .build());
        return "runners/announce";
    }
}
