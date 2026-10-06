package resolvenow.client;

import org.springframework.stereotype.Component;

@Component
public class NotificationClientFallback implements NotificationClient {
    @Override
    public void create(NotificationCreateRequest request) {
        System.err.println("notificationService unavailable — user " + request.userId()
                + " was not notified about complaint " + request.complaintId());
    }
}