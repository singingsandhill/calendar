package me.singingsandhill.calendar.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import me.singingsandhill.calendar.application.service.OwnerService;

@Controller
public class HomeController {

    private final OwnerService ownerService;

    public HomeController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/start")
    public String start(@RequestParam String ownerId) {
        ownerService.getOrCreateOwner(ownerId);
        return "redirect:/" + ownerId;
    }
}
