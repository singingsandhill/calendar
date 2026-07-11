package me.singingsandhill.calendar.datedate.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.singingsandhill.calendar.datedate.application.service.OwnerService;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@RestController
@RequestMapping("/api/me")
public class MeApiController {

    public record LinkedOwnerResponse(String ownerId, Long userId) {

        public static LinkedOwnerResponse from(Owner owner) {
            return new LinkedOwnerResponse(owner.getOwnerId(), owner.getUserId());
        }
    }

    private final OwnerService ownerService;

    public MeApiController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping("/owners/{ownerId}")
    public ResponseEntity<LinkedOwnerResponse> linkOwner(@PathVariable String ownerId,
                                                         Authentication authentication) {
        Long userId = AuthenticatedUsers.currentUserId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("authenticated kakao user required"));
        Owner owner = ownerService.linkOwnerToUser(ownerId, userId);
        return ResponseEntity.ok(LinkedOwnerResponse.from(owner));
    }
}
