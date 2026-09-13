package resolvenow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    private Long complaintId;
    private String department;
    private Long assignedTo;

    @Builder.Default
    private String status = "ASSIGNED";

    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();
}