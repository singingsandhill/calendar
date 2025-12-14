package me.singingsandhill.calendar.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import me.singingsandhill.calendar.application.service.OwnerService;
import me.singingsandhill.calendar.application.service.SeoService;

@Controller
public class HomeController {

    private final OwnerService ownerService;
    private final SeoService seoService;

    public HomeController(OwnerService ownerService, SeoService seoService) {
        this.ownerService = ownerService;
        this.seoService = seoService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("seo", seoService.getHomeSeo());
        return "index";
    }

    @PostMapping("/start")
    public String start(@RequestParam String ownerId) {
        ownerService.getOrCreateOwner(ownerId);
        return "redirect:/" + ownerId;
    }
}
