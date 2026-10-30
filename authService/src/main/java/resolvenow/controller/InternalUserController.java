package resolvenow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import resolvenow.repo.UserRepository;

import java.util.List;

/**
 * Internal endpoint — called only by assignmentService via Feign to resolve
 * which agent IDs belong to a given department. Not exposed to end users
 * (it sits behind the Gateway's JWT filter, but only internal Feign calls hit it).
 */
@RestController
@RequestMapping("/api/auth/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @Value("${internal.service.token}")
    private String internalToken;

    @GetMapping("/agents")
    public ResponseEntity<List<Long>> getAgentIdsByDepartment(
            @RequestParam String department,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (!internalToken.equals(token)) {
            return ResponseEntity.status(403).build();
        }
        List<Long> ids = userRepository.findByRoleAndDepartment("AGENT", department.toUpperCase())
                .stream()
                .filter(resolvenow.model.User::isEnabled)
                .map(u -> u.getUserId())
                .toList();
        return ResponseEntity.ok(ids);
    }
}
