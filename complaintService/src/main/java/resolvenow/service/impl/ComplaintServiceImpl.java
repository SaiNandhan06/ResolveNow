package resolvenow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resolvenow.client.AssignmentClient;
import resolvenow.client.AssignmentClient.AssignmentTriggerRequest;
import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;
import resolvenow.model.Complaint;
import resolvenow.repo.ComplaintRepository;
import resolvenow.service.ComplaintService;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AssignmentClient assignmentClient;

    @Override
    public ComplaintResponse createComplaint(Long userId, ComplaintRequest request) {
        Complaint complaint = Complaint.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .status("OPEN")
                .build();

        complaint = complaintRepository.save(complaint);

        assignmentClient.triggerAssignment(
                new AssignmentTriggerRequest(complaint.getComplaintId(), complaint.getCategory(), userId)
        );

        return toResponse(complaint);
    }

    @Override
    public ComplaintResponse getComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + complaintId));
        return toResponse(complaint);
    }

    @Override
    public ComplaintResponse updateStatus(Long complaintId, String status) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + complaintId));
        complaint.setStatus(status);
        complaint = complaintRepository.save(complaint);
        return toResponse(complaint);
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .userId(complaint.getUserId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt())
                .build();
    }
}