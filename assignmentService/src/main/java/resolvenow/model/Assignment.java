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
    private Long customerId;    // userId of the customer who raised the complaint
    private String department;
    private Long assignedTo;

    /**
     * ASSIGNED     — newly assigned, agent hasn't accepted yet
     * IN_PROGRESS  — agent accepted / started working
     * RESOLVED     — agent marked done
     * CLOSED       — this assignment was superseded by an escalation
     */
    @Builder.Default
    private String status = "ASSIGNED";

    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    /** Set when status transitions to RESOLVED or CLOSED */
    private LocalDateTime closedAt;
}