package com.inter.SistemaDeVisitas.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Exceções de regra de cadastro
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        // volta pro formulário de cadastro com a mensagem
        return "register";
    }

    // Acesso negado vai para 403
    @ExceptionHandler(AccessDeniedException.class)
    public String handleDenied(AccessDeniedException ex) {
        return "error/403";
    }

    // Qualquer outra exceção não prevista -> 500 custom
    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        // opcional: logar ex aqui
        model.addAttribute("errorMessage", "Ocorreu um erro inesperado.");
        return "error/500";
    }
}
