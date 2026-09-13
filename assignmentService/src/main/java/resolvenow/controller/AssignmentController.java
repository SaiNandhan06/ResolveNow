package resolvenow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resolvenow.dto.AssignmentTriggerRequest;
import resolvenow.service.AssignmentService;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<Void> assign(@RequestBody AssignmentTriggerRequest request) {
        assignmentService.autoAssign(request.complaintId(), request.category(), request.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<Object> getByComplaint(@PathVariable Long complaintId) {
        return ResponseEntity.ok(assignmentService.getByComplaintId(complaintId));
    }
}