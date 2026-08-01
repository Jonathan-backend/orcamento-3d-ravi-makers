package br.com.orcamento3d.budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BudgetRepository extends JpaRepository<Budget,Long>{
 List<Budget> findByOwnerEmailOrderByCreatedAtDesc(String email);
 Optional<Budget> findByIdAndOwnerEmail(Long id,String email);
}
