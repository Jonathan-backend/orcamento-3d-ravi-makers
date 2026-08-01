package br.com.orcamento3d.quote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
 Optional<PricingConfig> findByOwnerEmail(String email);
}
