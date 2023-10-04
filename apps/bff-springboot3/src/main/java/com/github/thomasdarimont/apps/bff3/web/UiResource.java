package com.github.thomasdarimont.apps.bff3.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
class UiResource {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appScript", "/app/app.js");
        return "/app/index";
    }
}
