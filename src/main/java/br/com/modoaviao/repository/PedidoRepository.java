package br.com.modoaviao.repository;

import br.com.modoaviao.model.Pedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByExternalReference(String externalReference);

    Optional<Pedido> findByOrderId(String orderId);
}
