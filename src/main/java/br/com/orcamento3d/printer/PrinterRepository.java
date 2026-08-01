package br.com.orcamento3d.printer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
    List<Printer> findByOwnerEmailOrderByName(String email);
    Optional<Printer> findByIdAndOwnerEmail(Long id, String email);
}
