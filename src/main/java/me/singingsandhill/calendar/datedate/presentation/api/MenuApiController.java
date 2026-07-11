package me.singingsandhill.calendar.datedate.presentation.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.datedate.application.service.MenuService;
import me.singingsandhill.calendar.datedate.application.service.UserActivityService;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.presentation.dto.request.MenuCreateRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.request.VoteRequest;
import me.singingsandhill.calendar.datedate.presentation.dto.response.MenuResponse;
import me.singingsandhill.calendar.datedate.presentation.support.AuthenticatedUsers;

@RestController
@RequestMapping("/api")
public class MenuApiController {

    private final MenuService menuService;
    private final UserActivityService userActivityService;

    public MenuApiController(MenuService menuService, UserActivityService userActivityService) {
        this.menuService = menuService;
        this.userActivityService = userActivityService;
    }

    @GetMapping("/schedules/{scheduleId}/menus")
    public ResponseEntity<List<MenuResponse>> getMenus(@PathVariable Long scheduleId) {
        List<Menu> menus = menuService.getMenusByScheduleId(scheduleId);
        List<MenuResponse> responses = menus.stream()
                .map(MenuResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/schedules/{scheduleId}/menus")
    public ResponseEntity<MenuResponse> addMenu(
            @PathVariable Long scheduleId,
            @Valid @RequestBody MenuCreateRequest request) {
        Menu menu = menuService.addMenu(scheduleId, request.name(), request.url());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MenuResponse.from(menu));
    }

    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/menus/{menuId}/votes")
    public ResponseEntity<MenuResponse> vote(
            @PathVariable Long menuId,
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {
        Menu menu = menuService.vote(menuId, request.voterName());
        AuthenticatedUsers.currentUserId(authentication).ifPresent(userId ->
                userActivityService.record(userId, ActivityType.MENU_VOTE,
                        menu.getScheduleId(), menuId, menu.getName()));
        return ResponseEntity.ok(MenuResponse.from(menu));
    }

    @DeleteMapping("/menus/{menuId}/votes/{voterName}")
    public ResponseEntity<MenuResponse> unvote(
            @PathVariable Long menuId,
            @PathVariable String voterName) {
        Menu menu = menuService.unvote(menuId, voterName);
        return ResponseEntity.ok(MenuResponse.from(menu));
    }
}
