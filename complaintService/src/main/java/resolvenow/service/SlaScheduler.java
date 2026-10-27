package resolvenow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import resolvenow.repo.ComplaintRepository;
import resolvenow.service.impl.ComplaintServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs every 5 minutes. Finds tickets whose SLA deadline has passed while still
 * ASSIGNED or IN_PROGRESS, and auto-escalates them to a different agent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final ComplaintRepository complaintRepository;
    private final ComplaintServiceImpl complaintServiceImpl;

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    public void checkSlaBreaches() {
        List<String> activeStatuses = List.of("ASSIGNED", "IN_PROGRESS");
        var overdue = complaintRepository.findByStatusInAndSlaDueAtBefore(activeStatuses, LocalDateTime.now());

        if (!overdue.isEmpty()) {
            log.info("SLA scheduler: {} overdue ticket(s) found — escalating", overdue.size());
        }

        for (var complaint : overdue) {
            try {
                complaintServiceImpl.slaEscalate(complaint.getComplaintId());
                log.info("Auto-escalated complaint {} (SLA breach)", complaint.getComplaintId());
            } catch (Exception e) {
                log.error("Failed to auto-escalate complaint {}: {}", complaint.getComplaintId(), e.getMessage());
            }
        }
    }
}
