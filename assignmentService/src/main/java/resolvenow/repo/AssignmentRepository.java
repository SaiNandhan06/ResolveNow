package resolvenow.repo;

import resolvenow.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /** Latest active assignment for a complaint */
    Optional<Assignment> findTopByComplaintIdOrderByAssignedAtDesc(Long complaintId);

    /** All assignments ever made for a complaint (audit trail) */
    List<Assignment> findByComplaintIdOrderByAssignedAtDesc(Long complaintId);

    /** Agent queue or history filtered by status list */
    List<Assignment> findByAssignedToAndStatusIn(Long agentId, List<String> statuses);

    /**
     * Count active tickets per agent for load-balancing.
     * Returns rows of [agentId (Long), activeCount (Long)].
     */
    @Query("SELECT a.assignedTo, COUNT(a) FROM Assignment a " +
           "WHERE a.assignedTo IN :agentIds AND a.status IN ('ASSIGNED','IN_PROGRESS') " +
           "GROUP BY a.assignedTo")
    List<Object[]> countActiveTicketsForAgents(@Param("agentIds") List<Long> agentIds);
}