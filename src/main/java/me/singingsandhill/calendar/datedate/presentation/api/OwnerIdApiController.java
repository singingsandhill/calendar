package me.singingsandhill.calendar.datedate.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.presentation.dto.response.OwnerIdSuggestionResponse;

/**
 * 아직 owner 가 아닌 "ID 후보" 를 다루므로 {@code /api/owners} 가 아닌 별도 경로를 쓴다.
 * {@code OwnerPathInterceptor} 가 {@code /api/owners/**} 의 첫 세그먼트를 ownerId 로 검증하며
 * 예약어를 거부하는데, "random" 이 바로 그 예약어다 — 보안 인터셉터에 구멍을 내지 않기 위한 분리.
 */
@RestController
@RequestMapping("/api/owner-ids")
public class OwnerIdApiController {

    private final OwnerService ownerService;

    public OwnerIdApiController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * 홈 "랜덤 생성" 버튼용. 반환 시점에 미사용임이 확인된 ID 만 준다.
     * DB 쓰기가 없어 ADR datedate/domain/0004 의 "GET 무변형" 원칙을 유지한다 —
     * 실제 점유는 {@code POST /start} 가 다시 확인하고 만든다.
     */
    @GetMapping("/random")
    public ResponseEntity<OwnerIdSuggestionResponse> suggestRandomOwnerId() {
        return ResponseEntity.ok(new OwnerIdSuggestionResponse(ownerService.generateAvailableOwnerId()));
    }
}
