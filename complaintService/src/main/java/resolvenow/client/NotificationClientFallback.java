package resolvenow.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationClientFallback implements NotificationClient {
    @Override
    public void create(NotificationCreateRequest request) {
        log.error("Notification service unavailable for complaint {}", request.complaintId());
    }
}
