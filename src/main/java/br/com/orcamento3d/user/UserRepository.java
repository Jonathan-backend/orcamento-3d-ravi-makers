package br.com.orcamento3d.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findFirstByRoleAndEnabledTrueOrderByIdAsc(Role role);
    List<User> findByAccountOwnerIdOrderByNameAsc(Long ownerId);
    long countByAccountOwnerIdAndEnabledTrue(Long ownerId);
    boolean existsByEmailIgnoreCase(String email);
}
