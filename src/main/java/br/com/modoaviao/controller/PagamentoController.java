package br.com.modoaviao.controller;

import br.com.modoaviao.dto.CriarPagamentoRequest;
import br.com.modoaviao.dto.CriarPagamentoResponse;
import br.com.modoaviao.service.MercadoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamento")
@RequiredArgsConstructor
public class PagamentoController {

    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/criar")
    public ResponseEntity<CriarPagamentoResponse> criar(@Valid @RequestBody CriarPagamentoRequest request) {
        return ResponseEntity.ok(mercadoPagoService.criarOrder(request.getEmail()));
    }
}
