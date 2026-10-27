package resolvenow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Called when a customer manually escalates a ticket, or when the SLA scheduler
 * detects a breach. Tells assignmentService to close the current assignment and
 * re-assign to a different agent in the same department.
 */
@FeignClient(name = "assignmentService")
public interface EscalationClient {

    @PostMapping("/api/assignments/{complaintId}/escalate")
    void escalate(@PathVariable("complaintId") Long complaintId,
                  @RequestBody EscalateRequest request);

    record EscalateRequest(Long requestedByUserId) {}
}
