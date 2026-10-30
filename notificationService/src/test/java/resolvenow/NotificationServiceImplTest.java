package resolvenow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resolvenow.model.Notification;
import resolvenow.repo.NotificationRepository;
import resolvenow.service.impl.NotificationServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationServiceImpl service;

    @Test
    void userCannotMarkAnotherUsersNotificationRead() {
        Notification notification = Notification.builder().notificationId(3L).userId(20L).build();
        when(notificationRepository.findById(3L)).thenReturn(Optional.of(notification));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.markRead(3L, 21L));
        verify(notificationRepository, never()).save(any());
    }
}
