package resolvenow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resolvenow.model.Notification;
import resolvenow.repo.NotificationRepository;
import resolvenow.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void create(Long userId, Long complaintId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .complaintId(complaintId)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Notification belongs to another user");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}