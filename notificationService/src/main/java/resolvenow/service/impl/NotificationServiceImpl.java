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
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public void markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}