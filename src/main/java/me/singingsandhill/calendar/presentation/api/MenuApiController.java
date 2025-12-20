package me.singingsandhill.calendar.presentation.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.application.service.MenuService;
import me.singingsandhill.calendar.domain.menu.Menu;
import me.singingsandhill.calendar.presentation.dto.request.MenuCreateRequest;
import me.singingsandhill.calendar.presentation.dto.request.VoteRequest;
import me.singingsandhill.calendar.presentation.dto.response.MenuResponse;

@RestController
@RequestMapping("/api")
public class MenuApiController {

    private final MenuService menuService;

    public MenuApiController(MenuService menuService) {
        this.menuService = menuService;
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
            @Valid @RequestBody VoteRequest request) {
        Menu menu = menuService.vote(menuId, request.voterName());
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
