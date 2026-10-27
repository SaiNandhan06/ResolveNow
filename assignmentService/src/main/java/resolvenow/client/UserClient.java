package resolvenow.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Calls authService to resolve which agent user IDs belong to a department.
 * Used at assignment-time and escalation-time to pick the least-loaded agent.
 */
@FeignClient(name = "authService")
public interface UserClient {

    @GetMapping("/api/auth/internal/agents")
    List<Long> getAgentIdsByDepartment(@RequestParam("department") String department);
}
