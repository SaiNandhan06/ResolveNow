package resolvenow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resolvenow.dto.AssignmentTriggerRequest;
import resolvenow.model.Assignment;
import resolvenow.service.AssignmentService;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    // ── Internal — called by complaintService via Feign ─────────────────────

    /** Auto-assign when a new complaint is created */
    @PostMapping
    public ResponseEntity<Void> assign(@RequestBody AssignmentTriggerRequest request) {
        assignmentService.autoAssign(request.complaintId(), request.category(), request.userId());
        return ResponseEntity.ok().build();
    }

    /** Escalate: close current assignment, re-assign to different agent */
    @PostMapping("/{complaintId}/escalate")
    public ResponseEntity<Void> escalate(
            @PathVariable Long complaintId,
            @RequestBody EscalateRequest request) {
        assignmentService.escalate(complaintId, request.requestedByUserId());
        return ResponseEntity.ok().build();
    }

    /** Get the current (latest) assignment for a complaint */
    @GetMapping("/{complaintId}")
    public ResponseEntity<Assignment> getByComplaint(@PathVariable Long complaintId) {
        return ResponseEntity.ok(assignmentService.getByComplaintId(complaintId));
    }

    // ── Agent-facing ─────────────────────────────────────────────────────────

    /** Agent's active ticket queue (ASSIGNED + IN_PROGRESS) */
    @GetMapping("/my")
    public ResponseEntity<List<Assignment>> getMyActive(
            @RequestHeader("X-User-Id") Long agentId) {
        return ResponseEntity.ok(assignmentService.getMyActiveTickets(agentId));
    }

    /** Agent's resolved / escalated-away history */
    @GetMapping("/my/history")
    public ResponseEntity<List<Assignment>> getMyHistory(
            @RequestHeader("X-User-Id") Long agentId) {
        return ResponseEntity.ok(assignmentService.getMyHistory(agentId));
    }

    /** Agent accepts a ticket → IN_PROGRESS */
    @PutMapping("/{assignmentId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable Long assignmentId,
            @RequestHeader("X-User-Id") Long agentId) {
        assignmentService.accept(assignmentId, agentId);
        return ResponseEntity.ok().build();
    }

    /** Agent resolves a ticket → RESOLVED */
    @PutMapping("/{assignmentId}/resolve")
    public ResponseEntity<Void> resolve(
            @PathVariable Long assignmentId,
            @RequestHeader("X-User-Id") Long agentId) {
        assignmentService.resolve(assignmentId, agentId);
        return ResponseEntity.ok().build();
    }

    record EscalateRequest(Long requestedByUserId) {}
}