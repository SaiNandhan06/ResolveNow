package resolvenow.service;

import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;

public interface ComplaintService {
    ComplaintResponse createComplaint(Long userId, ComplaintRequest request);
    ComplaintResponse getComplaint(Long complaintId);
    ComplaintResponse updateStatus(Long complaintId, String status);
}