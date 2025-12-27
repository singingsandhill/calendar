package me.singingsandhill.calendar.presentation.controller;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.application.service.RunService;
import me.singingsandhill.calendar.domain.runner.Run;
import me.singingsandhill.calendar.domain.runner.RunCategory;
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

    private final RunService runService;

    public RunnerAdminController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "runners/admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Run> runs = runService.getAllRuns();
        model.addAttribute("runs", runs.stream()
                .map(RunResponse::from)
                .collect(Collectors.toList()));
        return "runners/admin/dashboard";
    }

    @GetMapping("/runs/new")
    public String newRunForm(Model model) {
        model.addAttribute("categories", RunCategory.values());
        model.addAttribute("run", null);
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
