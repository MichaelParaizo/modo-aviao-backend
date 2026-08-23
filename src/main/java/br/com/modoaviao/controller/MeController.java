package br.com.modoaviao.controller;

import br.com.modoaviao.dto.MeResponse;
import br.com.modoaviao.exception.UsuarioNaoEncontradoException;
import br.com.modoaviao.model.Usuario;
import br.com.modoaviao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario autenticado nao encontrado: " + authentication.getName()));

        return new MeResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getWhatsapp(),
                usuario.isAcessoLiberado());
    }
}
