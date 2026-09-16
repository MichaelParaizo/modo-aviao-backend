package br.com.modoaviao.service;

import br.com.modoaviao.model.EmailAutorizado;
import br.com.modoaviao.repository.EmailAutorizadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponto unico da logica de "liberar acesso" (adicionar/remover email da
 * lista de autorizados) - usado tanto pelo admin (tela de Acessos, origem
 * "manual") quanto pelo webhook da Hotmart (origem "hotmart"), pra nao
 * duplicar a mesma checagem de idempotencia em dois lugares.
 */
@Service
@RequiredArgsConstructor
public class EmailAutorizadoService {

    private final EmailAutorizadoRepository emailAutorizadoRepository;

    @Transactional
    public void liberarEmail(String email, String origem) {
        String emailNormalizado = normalizar(email);

        if (!emailAutorizadoRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            EmailAutorizado autorizado = new EmailAutorizado();
            autorizado.setEmail(emailNormalizado);
            autorizado.setOrigem(origem);
            emailAutorizadoRepository.save(autorizado);
        }
    }

    @Transactional
    public void removerEmail(String email) {
        emailAutorizadoRepository.deleteByEmailIgnoreCase(normalizar(email));
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
