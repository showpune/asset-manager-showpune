package com.microsoft.migration.assets.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String redirectToStorage() {
        return "redirect:/storage";
    }
}