package br.com.modoaviao.controller.admin;

import br.com.modoaviao.model.EmailAutorizado;
import br.com.modoaviao.repository.EmailAutorizadoRepository;
import br.com.modoaviao.service.EmailAutorizadoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminAcessoController {

    private final EmailAutorizadoRepository emailAutorizadoRepository;
    private final EmailAutorizadoService emailAutorizadoService;

    @GetMapping("/admin/acessos")
    public String listar(Model model) {
        List<EmailAutorizado> emails = emailAutorizadoRepository.findAllByOrderByLiberadoEmDesc();
        model.addAttribute("emails", emails);
        return "admin/acessos-lista";
    }

    @PostMapping("/admin/acessos/adicionar")
    public String adicionar(@RequestParam String email) {
        emailAutorizadoService.liberarEmail(email, "manual");
        return "redirect:/admin/acessos";
    }

    @PostMapping("/admin/acessos/remover")
    public String remover(@RequestParam String email) {
        emailAutorizadoService.removerEmail(email);
        return "redirect:/admin/acessos";
    }
}
