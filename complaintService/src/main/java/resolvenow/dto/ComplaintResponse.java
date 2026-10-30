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
    private String priority;
    private String status;
    private int escalationCount;
    private LocalDateTime slaDueAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String resolutionNotes;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}