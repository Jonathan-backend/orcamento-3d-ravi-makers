package br.com.orcamento3d.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackReport,Long> {
    List<FeedbackReport> findTop20ByOwnerEmailOrderByCreatedAtDesc(String email);
    List<FeedbackReport> findTop200ByOrderByCreatedAtDesc();
    Optional<FeedbackReport> findByIdAndOwnerEmail(Long id,String email);
}
