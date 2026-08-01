package br.com.orcamento3d.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FilamentMovementRepository extends JpaRepository<FilamentMovement, Long> {
    List<FilamentMovement> findByFilamentIdOrderByCreatedAtDesc(Long filamentId);
    void deleteByFilamentId(Long filamentId);
}
