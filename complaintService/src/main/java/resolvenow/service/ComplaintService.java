package resolvenow.service;

import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse createComplaint(Long userId, ComplaintRequest request);
    ComplaintResponse getComplaint(Long complaintId);
    ComplaintResponse updateStatus(Long complaintId, String status);
    List<ComplaintResponse> getMyComplaints(Long userId);
    ComplaintResponse escalate(Long complaintId, Long requestedByUserId);
    ComplaintResponse close(Long complaintId, Long requestedByUserId);
    ComplaintResponse getComplaintForUser(Long complaintId, Long userId);
}