package resolvenow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import resolvenow.client.ComplaintClient;
import resolvenow.client.NotificationClient;
import resolvenow.client.NotificationClient.NotificationCreateRequest;
import resolvenow.client.UserClient;
import resolvenow.model.Assignment;
import resolvenow.repo.AssignmentRepository;
import resolvenow.service.AssignmentService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ComplaintClient complaintClient;
    private final NotificationClient notificationClient;
    private final UserClient userClient;

    private static final Map<String, String> CATEGORY_TO_DEPARTMENT = Map.of(
            "BILLING",   "BILLING",
            "TECHNICAL", "TECHNICAL",
            "HR",        "HR",
            "LOGISTICS", "LOGISTICS"
    );

    // ── Auto-assign on ticket creation ──────────────────────────────────────

    @Override
    public void autoAssign(Long complaintId, String category, Long userId) {
        String department = CATEGORY_TO_DEPARTMENT.getOrDefault(category.toUpperCase(), "GENERAL");
        Long agentId = pickLeastLoadedAgent(department, null);

        Assignment assignment = Assignment.builder()
                .complaintId(complaintId)
                .customerId(userId)
                .department(department)
                .assignedTo(agentId)
                .status("ASSIGNED")
                .build();

        assignmentRepository.save(assignment);
        complaintClient.updateStatus(complaintId, "ASSIGNED");

        // Notify customer
        notificationClient.create(new NotificationCreateRequest(
                userId, complaintId,
                "Your complaint has been assigned to the " + department + " department."));

        // Notify agent — only if we actually found one
        if (agentId != null) {
            notificationClient.create(new NotificationCreateRequest(
                    agentId, complaintId,
                    "You have been assigned a new ticket #" + complaintId + " [" + department + "]."));
        }
    }

    // ── Escalation ───────────────────────────────────────────────────────────

    @Override
    public void escalate(Long complaintId, Long requestedByUserId) {
        // Close current active assignment
        Assignment current = assignmentRepository
                .findTopByComplaintIdOrderByAssignedAtDesc(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("No active assignment for complaint " + complaintId));

        Long previousAgentId = current.getAssignedTo();
        Long customerId = current.getCustomerId();
        String department = current.getDepartment();

        current.setStatus("CLOSED");
        current.setClosedAt(LocalDateTime.now());
        assignmentRepository.save(current);

        // Pick a different agent
        Long newAgentId = pickLeastLoadedAgent(department, previousAgentId);

        Assignment newAssignment = Assignment.builder()
                .complaintId(complaintId)
                .customerId(customerId)
                .department(department)
                .assignedTo(newAgentId)
                .status("ASSIGNED")
                .build();
        assignmentRepository.save(newAssignment);

        complaintClient.updateStatus(complaintId, "ASSIGNED");

        // Notify customer — use customerId from the assignment record
        if (customerId != null) {
            notificationClient.create(new NotificationCreateRequest(
                    customerId, complaintId,
                    "Your complaint #" + complaintId + " has been escalated to another agent."));
        }

        // Notify new agent
        if (newAgentId != null) {
            notificationClient.create(new NotificationCreateRequest(
                    newAgentId, complaintId,
                    "Ticket #" + complaintId + " has been escalated to you [" + department + "]."));
        }
    }

    // ── Agent actions ────────────────────────────────────────────────────────

    @Override
    public void accept(Long assignmentId, Long agentId) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        validateOwner(assignment, agentId);
        if (!"ASSIGNED".equals(assignment.getStatus())) {
            throw new IllegalArgumentException("Only assigned tickets can be accepted");
        }

        assignment.setStatus("IN_PROGRESS");
        assignmentRepository.save(assignment);
        complaintClient.updateStatus(assignment.getComplaintId(), "IN_PROGRESS");

        notificationClient.create(new NotificationCreateRequest(
                assignment.getCustomerId(), assignment.getComplaintId(),
                "An agent has started working on your complaint #" + assignment.getComplaintId() + "."));
    }

    @Override
    public void resolve(Long assignmentId, Long agentId) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        validateOwner(assignment, agentId);
        if (!"IN_PROGRESS".equals(assignment.getStatus())) {
            throw new IllegalArgumentException("Only in-progress tickets can be resolved");
        }

        assignment.setStatus("RESOLVED");
        assignment.setClosedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        complaintClient.updateStatus(assignment.getComplaintId(), "RESOLVED");

        notificationClient.create(new NotificationCreateRequest(
                assignment.getCustomerId(), assignment.getComplaintId(),
                "Your complaint #" + assignment.getComplaintId() + " has been resolved."));
    }

    @Override
    public List<Assignment> getMyActiveTickets(Long agentId) {
        return assignmentRepository.findByAssignedToAndStatusIn(
                agentId, List.of("ASSIGNED", "IN_PROGRESS"));
    }

    @Override
    public List<Assignment> getMyHistory(Long agentId) {
        return assignmentRepository.findByAssignedToAndStatusIn(
                agentId, List.of("RESOLVED", "CLOSED"));
    }

    @Override
    public Assignment getByComplaintId(Long complaintId) {
        return assignmentRepository.findTopByComplaintIdOrderByAssignedAtDesc(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("No assignment for complaint: " + complaintId));
    }

    @Override
    public Assignment getByComplaintIdForCustomer(Long complaintId, Long customerId) {
        Assignment assignment = getByComplaintId(complaintId);
        if (!customerId.equals(assignment.getCustomerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Assignment belongs to another customer");
        }
        return assignment;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Picks the agent in the given department with the fewest active tickets.
     * Excludes excludeAgentId (used on escalation to avoid re-assigning to the same agent).
     * Returns null and logs a warning if no agents are registered for the department.
     */
    private Long pickLeastLoadedAgent(String department, Long excludeAgentId) {
        List<Long> candidates;
        try {
            candidates = userClient.getAgentIdsByDepartment(department);
        } catch (Exception e) {
            log.warn("Could not fetch agents for department {}: {}", department, e.getMessage());
            return null;
        }

        if (excludeAgentId != null) {
            candidates = candidates.stream()
                    .filter(id -> !id.equals(excludeAgentId))
                    .toList();
        }

        if (candidates.isEmpty()) {
            log.warn("No agents available for department {} (excludeAgent={})", department, excludeAgentId);
            return null;
        }

        // Build a count map: agentId -> activeTickets (default 0)
        Map<Long, Long> activeCountMap = new HashMap<>();
        for (Long id : candidates) activeCountMap.put(id, 0L);

        List<Object[]> counts = assignmentRepository.countActiveTicketsForAgents(candidates);
        for (Object[] row : counts) {
            Long agentId = (Long) row[0];
            Long count   = (Long) row[1];
            activeCountMap.put(agentId, count);
        }

        // Return the agent with the minimum active ticket count
        return activeCountMap.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Assignment findAssignmentOrThrow(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    private void validateOwner(Assignment assignment, Long agentId) {
        if (!agentId.equals(assignment.getAssignedTo())) {
            throw new IllegalArgumentException("Assignment " + assignment.getAssignmentId() +
                    " does not belong to agent " + agentId);
        }
    }
}