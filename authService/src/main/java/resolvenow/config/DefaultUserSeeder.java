package resolvenow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import resolvenow.model.User;
import resolvenow.repo.UserRepository;

@Configuration
@RequiredArgsConstructor
public class DefaultUserSeeder {

    private static final String[] DEPARTMENTS = {"BILLING", "TECHNICAL", "HR", "LOGISTICS", "GENERAL"};

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.user.password}")
    private String seedPassword;

    @Bean
    CommandLineRunner seedOperationalUsers() {
        return args -> {
            createIfMissing("admin", "admin@resolvenow.local", "ADMIN", null);
            for (String department : DEPARTMENTS) {
                createIfMissing("supervisor-" + department.toLowerCase(),
                        "supervisor-" + department.toLowerCase() + "@resolvenow.local",
                        "SUPERVISOR", department);
                for (int i = 1; i <= 2; i++) {
                    createIfMissing("agent-" + department.toLowerCase() + "-" + i,
                            "agent-" + department.toLowerCase() + "-" + i + "@resolvenow.local",
                            "AGENT", department);
                }
            }
        };
    }

    private void createIfMissing(String username, String email, String role, String department) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(seedPassword))
                    .role(role)
                    .department(department)
                    .enabled(true)
                    .build());
        }
    }
}
