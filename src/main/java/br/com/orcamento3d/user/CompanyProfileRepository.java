package br.com.orcamento3d.user;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile,Long>{Optional<CompanyProfile> findByOwnerEmail(String email);}
