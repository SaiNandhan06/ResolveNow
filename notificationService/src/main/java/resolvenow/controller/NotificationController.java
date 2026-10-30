package resolvenow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import resolvenow.model.Notification;
import resolvenow.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${internal.service.token}")
    private String internalToken;

    // ── Internal — called by assignmentService via Feign ────────────────────

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody NotificationCreateRequest request,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        if (request.userId() == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Notification user and message are required");
        }
        notificationService.create(request.userId(), request.complaintId(), request.message());
        return ResponseEntity.ok().build();
    }

    // ── User-facing ──────────────────────────────────────────────────────────

    /** Returns all notifications for the calling user (reads X-User-Id from Gateway) */
    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMy(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /** Legacy path — kept for backward compatibility */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requestingUserId) {
        if (!userId.equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notifications belong to another user");
        }
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    /** Mark a single notification as read */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }

    /** Mark all notifications as read for the calling user */
    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllRead(
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    public record NotificationCreateRequest(Long userId, Long complaintId, String message) {}

    private void requireInternal(String token) {
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal endpoint");
        }
    }
}