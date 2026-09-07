package br.com.modoaviao.repository;

import br.com.modoaviao.model.EmailAutorizado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailAutorizadoRepository extends JpaRepository<EmailAutorizado, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<EmailAutorizado> findByEmailIgnoreCase(String email);

    List<EmailAutorizado> findAllByOrderByLiberadoEmDesc();

    void deleteByEmailIgnoreCase(String email);
}
