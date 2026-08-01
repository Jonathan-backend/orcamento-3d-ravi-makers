package br.com.orcamento3d.inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ConsumableRepository extends JpaRepository<Consumable,Long>{
 List<Consumable> findByOwnerEmailOrderByCategoryAscNameAsc(String email);
 Optional<Consumable> findByIdAndOwnerEmail(Long id,String email);
}
