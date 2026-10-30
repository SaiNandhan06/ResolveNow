package resolvenow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;
import resolvenow.service.ComplaintService;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @Value("${internal.service.token}")
    private String internalToken;

    /** Customer: raise a new complaint */
    @PostMapping
    public ResponseEntity<ComplaintResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ComplaintRequest request) {
        requireRole(role, "CUSTOMER");
        return ResponseEntity.ok(complaintService.createComplaint(userId, request));
    }

    /** Any authenticated user: get a single complaint by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getById(@PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok("CUSTOMER".equals(role)
                ? complaintService.getComplaintForUser(id, userId)
                : complaintService.getComplaint(id));
    }

    /** Customer: list all their own complaints */
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMy(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "CUSTOMER");
        return ResponseEntity.ok(complaintService.getMyComplaints(userId));
    }

    /** Customer: manually escalate a ticket */
    @PutMapping("/{id}/escalate")
    public ResponseEntity<ComplaintResponse> escalate(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "CUSTOMER");
        return ResponseEntity.ok(complaintService.escalate(id, userId));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ComplaintResponse> close(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        requireRole(role, "CUSTOMER");
        return ResponseEntity.ok(complaintService.close(id, userId));
    }

    /**
     * Internal — called by assignmentService via Feign to update status
     * (ASSIGNED / IN_PROGRESS / RESOLVED / ESCALATED).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id, @RequestParam String status,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal endpoint");
        }
        return ResponseEntity.ok(complaintService.updateStatus(id, status));
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed");
        }
    }
}