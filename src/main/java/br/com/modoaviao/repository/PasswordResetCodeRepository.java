package br.com.modoaviao.repository;

import br.com.modoaviao.model.PasswordResetCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findByEmailAndCodeAndUsedFalse(String email, String code);
}
