package resolvenow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "complaintService")
public interface ComplaintClient {

    @PutMapping("/api/complaints/{id}/status")
    void updateStatus(@PathVariable("id") Long complaintId, @RequestParam String status);
}