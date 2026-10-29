package resolvenow.client;

import org.springframework.stereotype.Component;

@Component
public class AssignmentClientFallback implements AssignmentClient {
    @Override
    public void triggerAssignment(AssignmentTriggerRequest request) {
        System.err.println("assignmentService unavailable — complaint "
                + request.complaintId() + " stays OPEN, needs manual assignment");
    }
}