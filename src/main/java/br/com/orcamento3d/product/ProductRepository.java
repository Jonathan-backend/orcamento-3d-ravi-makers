package br.com.orcamento3d.product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,Long>{
 List<Product> findByOwnerEmailOrderByCreatedAtDesc(String email);
 List<Product> findByOwnerIdAndPublishedTrueOrderByFeaturedDescCreatedAtDesc(Long ownerId);
 Optional<Product> findByIdAndOwnerEmail(Long id,String email);
}
