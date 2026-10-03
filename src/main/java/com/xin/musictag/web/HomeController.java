package com.xin.musictag.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public final class HomeController {
    @GetMapping({"/", "/single"})
    public String home() { return "single"; }
}
