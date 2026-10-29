package resolvenow.client;

import org.springframework.stereotype.Component;

@Component
public class ComplaintClientFallback implements ComplaintClient {
    @Override
    public void updateStatus(Long complaintId, String status) {
        System.err.println("complaintService unavailable — could not update complaint "
                + complaintId + " to status " + status + ". Needs manual reconciliation.");
    }
}