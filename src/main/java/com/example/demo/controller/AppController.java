package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppController {

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/maquina")
    public String maquina() {
        return "maquina";
    }

    @GetMapping("/Corte")
    public String corte() {
        return "corte";
    }

    @GetMapping("/Empaque")
    public String empaque() {
        return "empaque";
    }

}