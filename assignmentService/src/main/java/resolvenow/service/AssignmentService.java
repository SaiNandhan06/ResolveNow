package resolvenow.service;

import resolvenow.model.Assignment;

import java.util.List;

public interface AssignmentService {
    void autoAssign(Long complaintId, String category, Long userId);
    Assignment getByComplaintId(Long complaintId);
    Assignment getByComplaintIdForCustomer(Long complaintId, Long customerId);
    void escalate(Long complaintId, Long requestedByUserId);
    List<Assignment> getMyActiveTickets(Long agentId);
    List<Assignment> getMyHistory(Long agentId);
    void accept(Long assignmentId, Long agentId);
    void resolve(Long assignmentId, Long agentId);
}