package resolvenow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resolvenow.client.AssignmentClient;
import resolvenow.client.AssignmentClient.AssignmentTriggerRequest;
import resolvenow.client.EscalationClient;
import resolvenow.client.EscalationClient.EscalateRequest;
import resolvenow.client.NotificationClient;
import resolvenow.client.NotificationClient.NotificationCreateRequest;
import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;
import resolvenow.model.Complaint;
import resolvenow.repo.ComplaintRepository;
import resolvenow.service.ComplaintService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AssignmentClient assignmentClient;
    private final EscalationClient escalationClient;
    private final NotificationClient notificationClient;

    // Category → priority mapping (auto-inferred, not customer-chosen)
    private static final Map<String, String> CATEGORY_PRIORITY = Map.of(
            "BILLING",   "HIGH",
            "TECHNICAL", "HIGH",
            "HR",        "MEDIUM",
            "LOGISTICS", "LOW"
    );

    // SLA windows in hours per priority
    private static final Map<String, Long> SLA_HOURS = Map.of(
            "HIGH",   2L,
            "MEDIUM", 8L,
            "LOW",    24L
    );

    @Override
    public ComplaintResponse createComplaint(Long userId, ComplaintRequest request) {
        String priority = CATEGORY_PRIORITY.getOrDefault(
                request.getCategory().toUpperCase(), "MEDIUM");
        long slaHours = SLA_HOURS.getOrDefault(priority, 8L);

        Complaint complaint = Complaint.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(priority)
                .status("OPEN")
                .slaDueAt(LocalDateTime.now().plusHours(slaHours))
                .build();

        complaint = complaintRepository.save(complaint);

        assignmentClient.triggerAssignment(
                new AssignmentTriggerRequest(complaint.getComplaintId(), complaint.getCategory(), userId)
        );

        return toResponse(complaint);
    }

    @Override
    public ComplaintResponse getComplaint(Long complaintId) {
        return toResponse(findOrThrow(complaintId));
    }

    @Override
    public ComplaintResponse getComplaintForUser(Long complaintId, Long userId) {
        Complaint complaint = findOrThrow(complaintId);
        if (!complaint.getUserId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Complaint does not belong to user");
        }
        return toResponse(complaint);
    }

    @Override
    public List<ComplaintResponse> getMyComplaints(Long userId) {
        return complaintRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ComplaintResponse updateStatus(Long complaintId, String status) {
        Complaint complaint = findOrThrow(complaintId);
        if (!isValidTransition(complaint.getStatus(), status)) {
            throw new IllegalArgumentException(
                    "Invalid complaint status transition: " + complaint.getStatus() + " -> " + status);
        }
        complaint.setStatus(status);
        complaint.setUpdatedAt(LocalDateTime.now());
        return toResponse(complaintRepository.save(complaint));
    }

    @Override
    public ComplaintResponse close(Long complaintId, Long requestedByUserId) {
        Complaint complaint = findOrThrow(complaintId);
        if (!complaint.getUserId().equals(requestedByUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Complaint does not belong to user");
        }
        if (!"RESOLVED".equals(complaint.getStatus())) {
            throw new IllegalArgumentException("Only resolved complaints can be closed");
        }
        complaint.setStatus("CLOSED");
        complaint.setClosedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        ComplaintResponse response = toResponse(complaintRepository.save(complaint));
        notificationClient.create(new NotificationCreateRequest(
                complaint.getUserId(), complaintId, "Your complaint has been closed."));
        return response;
    }

    @Override
    public ComplaintResponse escalate(Long complaintId, Long requestedByUserId) {
        Complaint complaint = findOrThrow(complaintId);
        if ("RESOLVED".equals(complaint.getStatus())) {
            throw new IllegalArgumentException("Cannot escalate a resolved complaint");
        }
        complaint.setStatus("ESCALATED");
        complaint.setEscalationCount(complaint.getEscalationCount() + 1);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        // Tell assignmentService to close current assignment and re-assign
        escalationClient.escalate(complaintId, new EscalateRequest(requestedByUserId));

        return toResponse(complaint);
    }

    // Called by the SLA scheduler — same logic as customer escalation but no HTTP context
    public void slaEscalate(Long complaintId) {
        Complaint complaint = findOrThrow(complaintId);
        complaint.setStatus("ESCALATED");
        complaint.setEscalationCount(complaint.getEscalationCount() + 1);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);
        escalationClient.escalate(complaintId, new EscalateRequest(null));
    }

    private Complaint findOrThrow(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Complaint not found: " + id));
    }

    private boolean isValidTransition(String current, String next) {
        return ("OPEN".equals(current) && "ASSIGNED".equals(next))
                || ("ESCALATED".equals(current) && "ASSIGNED".equals(next))
                || ("ASSIGNED".equals(current) && "IN_PROGRESS".equals(next))
                || ("IN_PROGRESS".equals(current) && "RESOLVED".equals(next));
    }

    private ComplaintResponse toResponse(Complaint c) {
        return ComplaintResponse.builder()
                .complaintId(c.getComplaintId())
                .userId(c.getUserId())
                .title(c.getTitle())
                .description(c.getDescription())
                .category(c.getCategory())
                .priority(c.getPriority())
                .status(c.getStatus())
                .escalationCount(c.getEscalationCount())
                .slaDueAt(c.getSlaDueAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .resolutionNotes(c.getResolutionNotes())
                .resolvedBy(c.getResolvedBy())
                .resolvedAt(c.getResolvedAt())
                .closedAt(c.getClosedAt())
                .build();
    }
}