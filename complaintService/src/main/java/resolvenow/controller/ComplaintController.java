package resolvenow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;
import resolvenow.service.ComplaintService;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    /** Customer: raise a new complaint */
    @PostMapping
    public ResponseEntity<ComplaintResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ComplaintRequest request) {
        return ResponseEntity.ok(complaintService.createComplaint(userId, request));
    }

    /** Any authenticated user: get a single complaint by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaint(id));
    }

    /** Customer: list all their own complaints */
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMy(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(complaintService.getMyComplaints(userId));
    }

    /** Customer: manually escalate a ticket */
    @PutMapping("/{id}/escalate")
    public ResponseEntity<ComplaintResponse> escalate(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(complaintService.escalate(id, userId));
    }

    /**
     * Internal — called by assignmentService via Feign to update status
     * (ASSIGNED / IN_PROGRESS / RESOLVED / ESCALATED).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(complaintService.updateStatus(id, status));
    }
}