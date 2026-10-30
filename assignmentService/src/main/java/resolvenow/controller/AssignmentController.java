package resolvenow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import resolvenow.dto.AssignmentTriggerRequest;
import resolvenow.model.Assignment;
import resolvenow.service.AssignmentService;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Value("${internal.service.token}")
    private String internalToken;

    // ── Internal — called by complaintService via Feign ─────────────────────

    /** Auto-assign when a new complaint is created */
    @PostMapping
    public ResponseEntity<Void> assign(@RequestBody AssignmentTriggerRequest request,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        assignmentService.autoAssign(request.complaintId(), request.category(), request.userId());
        return ResponseEntity.ok().build();
    }

    /** Escalate: close current assignment, re-assign to different agent */
    @PostMapping("/{complaintId}/escalate")
    public ResponseEntity<Void> escalate(
            @PathVariable Long complaintId,
            @RequestBody EscalateRequest request,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        assignmentService.escalate(complaintId, request.requestedByUserId());
        return ResponseEntity.ok().build();
    }

    /** Get the current (latest) assignment for a complaint */
    @GetMapping("/{complaintId}")
    public ResponseEntity<Assignment> getByComplaint(@PathVariable Long complaintId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        if (!List.of("CUSTOMER", "AGENT", "SUPERVISOR", "ADMIN").contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed");
        }
        return ResponseEntity.ok("CUSTOMER".equals(role)
                ? assignmentService.getByComplaintIdForCustomer(complaintId, userId)
                : assignmentService.getByComplaintId(complaintId));
    }

    // ── Agent-facing ─────────────────────────────────────────────────────────

    /** Agent's active ticket queue (ASSIGNED + IN_PROGRESS) */
    @GetMapping("/my")
    public ResponseEntity<List<Assignment>> getMyActive(
            @RequestHeader("X-User-Id") Long agentId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "AGENT");
        return ResponseEntity.ok(assignmentService.getMyActiveTickets(agentId));
    }

    /** Agent's resolved / escalated-away history */
    @GetMapping("/my/history")
    public ResponseEntity<List<Assignment>> getMyHistory(
            @RequestHeader("X-User-Id") Long agentId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "AGENT");
        return ResponseEntity.ok(assignmentService.getMyHistory(agentId));
    }

    /** Agent accepts a ticket → IN_PROGRESS */
    @PutMapping("/{assignmentId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable Long assignmentId,
            @RequestHeader("X-User-Id") Long agentId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "AGENT");
        assignmentService.accept(assignmentId, agentId);
        return ResponseEntity.ok().build();
    }

    /** Agent resolves a ticket → RESOLVED */
    @PutMapping("/{assignmentId}/resolve")
    public ResponseEntity<Void> resolve(
            @PathVariable Long assignmentId,
            @RequestHeader("X-User-Id") Long agentId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "AGENT");
        assignmentService.resolve(assignmentId, agentId);
        return ResponseEntity.ok().build();
    }

    record EscalateRequest(Long requestedByUserId) {}

    private void requireInternal(String token) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal endpoint");
        }
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed");
        }
    }
}