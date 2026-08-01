package br.com.orcamento3d.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @EntityGraph(attributePaths = {"owner", "printer", "customer"})
    List<Quote> findByOwnerEmailOrderByCreatedAtDesc(String email);
}
