package resolvenow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificationService")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    void create(@RequestBody NotificationCreateRequest request);

    record NotificationCreateRequest(Long userId, Long complaintId, String message) {}
}