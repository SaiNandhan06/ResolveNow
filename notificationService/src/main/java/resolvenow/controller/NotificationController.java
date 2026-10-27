package resolvenow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resolvenow.model.Notification;
import resolvenow.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Internal — called by assignmentService via Feign ────────────────────

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody NotificationCreateRequest request) {
        // userId may be null for "notify the complaint owner" calls where assignmentService
        // doesn't know the customer userId — skip those silently (notification needs userId)
        if (request.userId() == null) {
            return ResponseEntity.ok().build();
        }
        notificationService.create(request.userId(), request.complaintId(), request.message());
        return ResponseEntity.ok().build();
    }

    // ── User-facing ──────────────────────────────────────────────────────────

    /** Returns all notifications for the calling user (reads X-User-Id from Gateway) */
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMy(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /** Legacy path — kept for backward compatibility */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /** Mark a single notification as read */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok().build();
    }

    /** Mark all notifications as read for the calling user */
    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllRead(
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    record NotificationCreateRequest(Long userId, Long complaintId, String message) {}
}