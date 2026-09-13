package resolvenow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "assignmentService")
public interface AssignmentClient {

    @PostMapping("/api/assignments")
    void triggerAssignment(@RequestBody AssignmentTriggerRequest request);

    record AssignmentTriggerRequest(Long complaintId, String category, Long userId) {}
}