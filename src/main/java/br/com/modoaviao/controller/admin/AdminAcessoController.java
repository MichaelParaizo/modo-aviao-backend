package br.com.modoaviao.controller.admin;

import br.com.modoaviao.model.EmailAutorizado;
import br.com.modoaviao.repository.EmailAutorizadoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminAcessoController {

    private final EmailAutorizadoRepository emailAutorizadoRepository;

    @GetMapping("/admin/acessos")
    public String listar(Model model) {
        List<EmailAutorizado> emails = emailAutorizadoRepository.findAllByOrderByLiberadoEmDesc();
        model.addAttribute("emails", emails);
        return "admin/acessos-lista";
    }

    @PostMapping("/admin/acessos/adicionar")
    @Transactional
    public String adicionar(@RequestParam String email, Model model) {
        String emailNormalizado = normalizar(email);

        if (!emailAutorizadoRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            EmailAutorizado autorizado = new EmailAutorizado();
            autorizado.setEmail(emailNormalizado);
            autorizado.setOrigem("manual");
            emailAutorizadoRepository.save(autorizado);
        }

        return "redirect:/admin/acessos";
    }

    @PostMapping("/admin/acessos/remover")
    @Transactional
    public String remover(@RequestParam String email) {
        emailAutorizadoRepository.deleteByEmailIgnoreCase(normalizar(email));
        return "redirect:/admin/acessos";
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
