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

    @Builder.Default
    private String status = "OPEN";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}