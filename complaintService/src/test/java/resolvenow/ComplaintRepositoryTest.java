package resolvenow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import resolvenow.model.Complaint;
import resolvenow.repo.ComplaintRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ComplaintRepositoryTest {

    @Autowired ComplaintRepository repository;

    @Test
    void findsComplaintsByOwner() {
        repository.save(Complaint.builder().userId(10L).title("One").description("A").category("HR").build());
        repository.save(Complaint.builder().userId(11L).title("Two").description("B").category("HR").build());

        assertEquals(1, repository.findByUserId(10L).size());
    }
}
