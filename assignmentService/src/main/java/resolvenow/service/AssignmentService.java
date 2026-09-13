package resolvenow.service;

public interface AssignmentService {
    void autoAssign(Long complaintId, String category, Long userId);
    Object getByComplaintId(Long complaintId);
}