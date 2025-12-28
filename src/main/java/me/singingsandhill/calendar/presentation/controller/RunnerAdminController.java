package me.singingsandhill.calendar.presentation.controller;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.application.service.RunService;
import me.singingsandhill.calendar.domain.runner.Run;
import me.singingsandhill.calendar.domain.runner.RunCategory;
import me.singingsandhill.calendar.presentation.dto.SeoMetadata;
import me.singingsandhill.calendar.presentation.dto.request.RunCreateRequest;
import me.singingsandhill.calendar.presentation.dto.response.RunResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/runners/admin")
public class RunnerAdminController {

    private static final String BASE_URL = "https://datedate.me";
    private static final String OG_IMAGE = "https://datedate.me/image/crew_logo.png";

    private final RunService runService;

    public RunnerAdminController(RunService runService) {
        this.runService = runService;
    }

    private SeoMetadata createAdminSeo(String title) {
        return SeoMetadata.builder()
                .title(title + " - 97 Runners 관리자")
                .description("97 Runners 러닝 크루 관리자 페이지")
                .keywords("97 runners, 관리자")
                .robots("noindex, nofollow")
                .canonical(BASE_URL + "/runners/admin")
                .ogImage(OG_IMAGE)
                .build();
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        model.addAttribute("seo", createAdminSeo("로그인"));
        return "runners/admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Run> runs = runService.getAllRuns();
        model.addAttribute("runs", runs.stream()
                .map(RunResponse::from)
                .collect(Collectors.toList()));
        model.addAttribute("seo", createAdminSeo("대시보드"));
        return "runners/admin/dashboard";
    }

    @GetMapping("/runs/new")
    public String newRunForm(Model model) {
        model.addAttribute("categories", RunCategory.values());
        model.addAttribute("run", null);
        model.addAttribute("seo", createAdminSeo("런 생성"));
        return "runners/admin/run-form";
    }

    @PostMapping("/runs")
    public String createRun(@Valid @ModelAttribute RunCreateRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", RunCategory.values());
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("seo", createAdminSeo("런 생성"));
            return "runners/admin/run-form";
        }

        runService.createRun(
            request.date(),
            request.time(),
            request.location(),
            RunCategory.valueOf(request.category())
        );

        redirectAttributes.addFlashAttribute("message", "런이 생성되었습니다.");
        return "redirect:/runners/admin";
    }

    @GetMapping("/runs/{id}/edit")
    public String editRunForm(@PathVariable Long id, Model model) {
        Run run = runService.getRunById(id);
        model.addAttribute("run", RunResponse.from(run));
        model.addAttribute("categories", RunCategory.values());
        model.addAttribute("seo", createAdminSeo("런 수정"));
        return "runners/admin/run-form";
    }

    @PostMapping("/runs/{id}")
    public String updateRun(@PathVariable Long id,
                            @Valid @ModelAttribute RunCreateRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", RunCategory.values());
            model.addAttribute("run", runService.getRunById(id));
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("seo", createAdminSeo("런 수정"));
            return "runners/admin/run-form";
        }

        runService.updateRun(
            id,
            request.date(),
            request.time(),
            request.location(),
            RunCategory.valueOf(request.category())
        );

        redirectAttributes.addFlashAttribute("message", "런이 수정되었습니다.");
        return "redirect:/runners/admin";
    }

    @PostMapping("/runs/{id}/delete")
    public String deleteRun(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        runService.deleteRun(id);
        redirectAttributes.addFlashAttribute("message", "런이 삭제되었습니다.");
        return "redirect:/runners/admin";
    }
}
