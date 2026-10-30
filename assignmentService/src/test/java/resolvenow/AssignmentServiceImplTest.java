package resolvenow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resolvenow.client.ComplaintClient;
import resolvenow.client.NotificationClient;
import resolvenow.client.UserClient;
import resolvenow.model.Assignment;
import resolvenow.repo.AssignmentRepository;
import resolvenow.service.impl.AssignmentServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock AssignmentRepository assignmentRepository;
    @Mock ComplaintClient complaintClient;
    @Mock NotificationClient notificationClient;
    @Mock UserClient userClient;
    @InjectMocks AssignmentServiceImpl service;

    @Test
    void assignsLeastLoadedAgentInCategoryDepartment() {
        when(userClient.getAgentIdsByDepartment("BILLING")).thenReturn(List.of(11L, 12L));
        when(assignmentRepository.countActiveTicketsForAgents(List.of(11L, 12L)))
                .thenReturn(List.of(new Object[]{11L, 2L}, new Object[]{12L, 0L}));

        service.autoAssign(4L, "BILLING", 9L);

        verify(assignmentRepository).save(argThat(a -> a.getAssignedTo().equals(12L)
                && a.getCustomerId().equals(9L)
                && a.getStatus().equals("ASSIGNED")));
        verify(complaintClient).updateStatus(4L, "ASSIGNED");
        verify(notificationClient, times(2)).create(any());
    }

    @Test
    void onlyAssignedTicketsCanBeAcceptedByOwner() {
        Assignment assignment = Assignment.builder().assignmentId(8L).assignedTo(12L)
                .status("IN_PROGRESS").build();
        when(assignmentRepository.findById(8L)).thenReturn(Optional.of(assignment));

        assertThrows(IllegalArgumentException.class, () -> service.accept(8L, 12L));
        verify(assignmentRepository, never()).save(any());
    }
}
