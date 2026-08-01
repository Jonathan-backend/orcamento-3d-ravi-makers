package br.com.orcamento3d.customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CustomerRepository extends JpaRepository<Customer,Long>{
    List<Customer> findByOwnerEmailOrderByNameAsc(String email);
    Optional<Customer> findByIdAndOwnerEmail(Long id,String email);
}
