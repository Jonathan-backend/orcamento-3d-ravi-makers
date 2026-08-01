package br.com.orcamento3d.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FilamentRepository extends JpaRepository<Filament, Long> {
    List<Filament> findByOwnerEmailOrderByBrandAscMaterialAsc(String email);
    Optional<Filament> findByIdAndOwnerEmail(Long id, String email);
}
