package resolvenow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import resolvenow.controller.ComplaintController;
import resolvenow.dto.ComplaintRequest;
import resolvenow.dto.ComplaintResponse;
import resolvenow.service.ComplaintService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintControllerTest {

    @Mock ComplaintService complaintService;
    private ComplaintController controller;

    @BeforeEach
    void setUp() {
        controller = new ComplaintController(complaintService);
        ReflectionTestUtils.setField(controller, "internalToken", "internal");
    }

    @Test
    void customerCanCreateComplaint() {
        ComplaintRequest request = new ComplaintRequest();
        ComplaintResponse response = ComplaintResponse.builder().complaintId(1L).status("OPEN").build();
        when(complaintService.createComplaint(4L, request)).thenReturn(response);

        assertEquals(response, controller.create(4L, "CUSTOMER", request).getBody());
        verify(complaintService).createComplaint(4L, request);
    }

    @Test
    void agentCannotCreateComplaint() {
        assertThrows(ResponseStatusException.class,
                () -> controller.create(4L, "AGENT", new ComplaintRequest()));
        verifyNoInteractions(complaintService);
    }

    @Test
    void statusCallbackRequiresInternalToken() {
        assertThrows(ResponseStatusException.class,
                () -> controller.updateStatus(1L, "ASSIGNED", "wrong"));
        verifyNoInteractions(complaintService);
    }
}
