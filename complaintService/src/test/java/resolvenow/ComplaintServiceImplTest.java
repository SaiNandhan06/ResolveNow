package resolvenow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resolvenow.client.AssignmentClient;
import resolvenow.client.EscalationClient;
import resolvenow.client.NotificationClient;
import resolvenow.dto.ComplaintRequest;
import resolvenow.model.Complaint;
import resolvenow.repo.ComplaintRepository;
import resolvenow.service.impl.ComplaintServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock AssignmentClient assignmentClient;
    @Mock EscalationClient escalationClient;
    @Mock NotificationClient notificationClient;
    @InjectMocks ComplaintServiceImpl service;

    @Test
    void createsComplaintWithPriorityAndSlaAndTriggersAssignment() {
        ComplaintRequest request = new ComplaintRequest();
        request.setTitle("Invoice mismatch");
        request.setDescription("The amount is wrong");
        request.setCategory("BILLING");

        Complaint saved = Complaint.builder().complaintId(7L).userId(3L)
                .title(request.getTitle()).description(request.getDescription())
                .category(request.getCategory()).priority("HIGH").status("OPEN")
                .slaDueAt(java.time.LocalDateTime.now().plusHours(2)).build();
        when(complaintRepository.save(any(Complaint.class))).thenReturn(saved);

        var response = service.createComplaint(3L, request);

        assertEquals("HIGH", response.getPriority());
        assertEquals("OPEN", response.getStatus());
        assertNotNull(response.getSlaDueAt());
        verify(assignmentClient).triggerAssignment(any());
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Complaint complaint = Complaint.builder().complaintId(1L).status("OPEN").build();
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, "RESOLVED"));
        verify(complaintRepository, never()).save(any());
    }

    @Test
    void rejectsCustomerReadingAnotherUsersComplaint() {
        Complaint complaint = Complaint.builder().complaintId(1L).userId(10L).build();
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.getComplaintForUser(1L, 11L));
    }
}
