package br.com.agendafacil.controller;

import br.com.agendafacil.exception.BusinessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public String business(BusinessException ex, Model model) {
        model.addAttribute("title", "Não foi possível continuar");
        model.addAttribute("message", ex.getMessage());
        return "public/error";
    }

    @ExceptionHandler(Exception.class)
    public String generic(Exception ex, Model model) {
        model.addAttribute("title", "Algo não saiu como esperado");
        model.addAttribute("message", "Tente novamente em alguns instantes. Se continuar acontecendo, fale com o suporte.");
        return "public/error";
    }
}
