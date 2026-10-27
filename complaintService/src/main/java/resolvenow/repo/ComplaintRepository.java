package resolvenow.repo;

import resolvenow.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserId(Long userId);

    /** Used by SLA scheduler: find tickets overdue and still active */
    List<Complaint> findByStatusInAndSlaDueAtBefore(List<String> statuses, LocalDateTime now);
}