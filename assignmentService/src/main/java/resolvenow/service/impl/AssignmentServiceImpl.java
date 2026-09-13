package resolvenow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resolvenow.client.ComplaintClient;
import resolvenow.client.NotificationClient;
import resolvenow.client.NotificationClient.NotificationCreateRequest;
import resolvenow.model.Assignment;
import resolvenow.repo.AssignmentRepository;
import resolvenow.service.AssignmentService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ComplaintClient complaintClient;
    private final NotificationClient notificationClient;

    private static final Map<String, String> CATEGORY_TO_DEPARTMENT = Map.of(
            "BILLING", "BILLING",
            "TECHNICAL", "TECHNICAL",
            "HR", "HR",
            "LOGISTICS", "LOGISTICS"
    );

    @Override
    public void autoAssign(Long complaintId, String category, Long userId) {
        String department = CATEGORY_TO_DEPARTMENT.getOrDefault(category.toUpperCase(), "GENERAL");

        Assignment assignment = Assignment.builder()
                .complaintId(complaintId)
                .department(department)
                .assignedTo(1L) // TODO: replace with real agent-selection logic (round-robin / least-loaded)
                .status("ASSIGNED")
                .build();

        assignmentRepository.save(assignment);

        complaintClient.updateStatus(complaintId, "ASSIGNED");

        notificationClient.create(new NotificationCreateRequest(
                userId, complaintId, "Your complaint has been assigned to " + department));
    }

    @Override
    public Object getByComplaintId(Long complaintId) {
        return assignmentRepository.findByComplaintId(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("No assignment for complaint: " + complaintId));
    }
}