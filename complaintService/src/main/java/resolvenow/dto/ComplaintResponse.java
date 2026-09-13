package resolvenow.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ComplaintResponse {
    private Long complaintId;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private String status;
    private LocalDateTime createdAt;
}