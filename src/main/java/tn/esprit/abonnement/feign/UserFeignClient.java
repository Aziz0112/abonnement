package tn.esprit.abonnement.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.abonnement.dto.UserDTO;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable Long id);
}
