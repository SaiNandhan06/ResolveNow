package resolvenow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;

    private Long userId;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    /** AUTO-INFERRED from category at creation: HIGH / MEDIUM / LOW */
    @Builder.Default
    private String priority = "MEDIUM";

    @Builder.Default
    private String status = "OPEN";

    /** SLA deadline — set at creation based on priority */
    private LocalDateTime slaDueAt;

    /** How many times this ticket has been escalated */
    @Builder.Default
    private int escalationCount = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private String resolutionNotes;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}