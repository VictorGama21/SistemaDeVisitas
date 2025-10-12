package com.inter.SistemaDeVisitas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/loja/visitas")
public class LojaVisitaController {

  @GetMapping
  public String listar() {
    return "loja/visitas";
  }
}
