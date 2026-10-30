package resolvenow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import resolvenow.controller.NotificationController;
import resolvenow.service.NotificationService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock NotificationService notificationService;
    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        ReflectionTestUtils.setField(controller, "internalToken", "internal");
    }

    @Test
    void notificationCreationRequiresInternalToken() {
        assertThrows(ResponseStatusException.class,
                () -> controller.create(new NotificationController.NotificationCreateRequest(2L, 3L, "hello"), "wrong"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void userCannotReadAnotherUsersNotifications() {
        assertThrows(ResponseStatusException.class,
                () -> controller.getByUser(2L, 3L));
        verifyNoInteractions(notificationService);
    }
}
