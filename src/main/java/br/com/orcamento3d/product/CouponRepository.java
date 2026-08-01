package br.com.orcamento3d.product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CouponRepository extends JpaRepository<Coupon,Long>{
 List<Coupon> findByOwnerEmailOrderByCode(String email);
 Optional<Coupon> findByIdAndOwnerEmail(Long id,String email);
 Optional<Coupon> findByOwnerIdAndCodeIgnoreCaseAndActiveTrue(Long ownerId,String code);
}
