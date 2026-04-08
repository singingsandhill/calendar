package me.singingsandhill.calendar.datedate.presentation.controller;

import me.singingsandhill.calendar.datedate.application.service.SeoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/use-cases")
public class UseCaseController {

    private final SeoService seoService;
    private final Map<String, UseCaseData> useCases;

    public UseCaseController(SeoService seoService) {
        this.seoService = seoService;
        this.useCases = new LinkedHashMap<>();

        useCases.put("friend-meetup", new UseCaseData(
            "친구 모임 약속 잡기",
            "친구들과 모임 날짜를 쉽게 정하세요. 동창 모임, 정기 모임, 생일 파티 등 여러명의 일정을 한번에 조율할 수 있습니다.",
            "college-friends",
            "동창 모임",
            "\"언제 돼?\" 카톡방에서 끝없이 반복되는 질문, 이제 그만하세요. DateDate 링크 하나면 친구들의 가능한 날짜가 한눈에 보입니다. "
                + "동창 모임, 정기 모임, 생일 파티, 동호회 번개 등 어떤 모임이든 링크를 공유하면 각자 되는 날짜를 선택하고, "
                + "가장 많은 친구가 가능한 날이 자동으로 표시됩니다. 장소와 메뉴까지 투표로 정할 수 있어 "
                + "\"어디서 만날까?\", \"뭐 먹을까?\"까지 한번에 해결됩니다. 가입도 앱 설치도 필요 없으니 부담 없이 시작하세요."
        ));

        useCases.put("team-meeting", new UseCaseData(
            "팀 회의 일정 조율",
            "바쁜 팀원들의 회의 시간을 효율적으로 잡으세요. 킥오프, 정기 회의, 리뷰 미팅 일정을 가입 없이 조율합니다.",
            "marketing-team",
            "마케팅팀 주간 회의",
            "프로젝트 킥오프, 스프린트 리뷰, 정기 회의... 바쁜 팀원들의 일정을 맞추는 건 쉽지 않습니다. "
                + "DateDate는 별도 가입이나 로그인 없이 링크만 공유하면 팀원들이 바로 가능한 날짜를 선택할 수 있습니다. "
                + "최대 8명까지 참여 가능하며, 각 참여자마다 고유 색상이 부여되어 누가 언제 가능한지 캘린더에서 직관적으로 확인됩니다. "
                + "슬랙, 이메일, 카카오톡 등 어떤 채널로든 링크를 공유할 수 있어 팀 커뮤니케이션 도구에 구애받지 않습니다."
        ));

        useCases.put("travel-planning", new UseCaseData(
            "여행 날짜 맞추기",
            "가족 여행, 친구 여행 날짜를 쉽게 맞추세요. 각자의 휴가 일정을 한곳에 모아 겹치는 날을 찾을 수 있습니다.",
            "japan-trip-2025",
            "일본 여행 계획",
            "여행은 가고 싶은데 날짜가 안 맞는다? DateDate로 여행 멤버들의 가능한 날짜를 한번에 모으세요. "
                + "각자 휴가 일정이나 가능한 날짜를 캘린더에서 클릭하면, 가장 많은 사람이 겹치는 날짜가 바로 보입니다. "
                + "가족 여행, 해외여행, 국내 여행, 워크숍 등 어떤 여행이든 활용 가능합니다. "
                + "여행지 투표 기능으로 \"어디로 갈까?\"도 함께 정할 수 있고, 맛집이나 관광지 링크를 메뉴에 첨부하면 "
                + "여행 계획까지 한 페이지에서 관리할 수 있습니다."
        ));

        useCases.put("study-group", new UseCaseData(
            "스터디 그룹 일정",
            "스터디, 독서 모임, 동아리 정기 모임 날짜를 쉽게 잡으세요. 각자 되는 날을 한곳에 모아 정기 일정을 결정합니다.",
            "coding-study",
            "코딩 스터디",
            "스터디 그룹의 정기 모임 날짜를 정하는 것, 생각보다 어렵죠? "
                + "DateDate를 사용하면 스터디원들이 각자 가능한 날짜를 선택하고, 가장 많은 사람이 모일 수 있는 날을 쉽게 찾을 수 있습니다. "
                + "코딩 스터디, 독서 모임, 어학 스터디, 자격증 스터디, 대학 동아리 등 어떤 그룹이든 활용 가능합니다. "
                + "모임 장소도 투표로 정할 수 있어 카페, 스터디룸, 도서관 중 어디서 만날지도 한번에 결정하세요. "
                + "매월 새로운 일정을 만들어 정기적으로 활용할 수 있습니다."
        ));
    }

    @GetMapping("/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        UseCaseData data = useCases.get(slug);
        if (data == null) {
            return "redirect:/";
        }

        model.addAttribute("seo", seoService.getUseCaseSeo(slug, data.title(), data.seoDescription()));
        model.addAttribute("useCase", data);
        model.addAttribute("allUseCases", useCases);
        model.addAttribute("currentSlug", slug);
        return "use-cases/detail";
    }

    public record UseCaseData(
        String title,
        String seoDescription,
        String exampleId,
        String exampleLabel,
        String content
    ) {}
}
