package resolvenow.service;

import resolvenow.model.Notification;
import java.util.List;

public interface NotificationService {
    void create(Long userId, Long complaintId, String message);
    List<Notification> getByUser(Long userId);
    void markRead(Long notificationId);
}