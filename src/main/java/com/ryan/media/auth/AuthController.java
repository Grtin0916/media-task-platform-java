package com.ryan.media.auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(
            new LoginResponse(
                "week06-skeleton",
                false,
                null,
                "JWT issuing is not implemented yet; this endpoint freezes the auth contract boundary first."
            )
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
            "authenticated", authentication != null && authentication.isAuthenticated(),
            "name", authentication.getName(),
            "authorities", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList()
        );
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String mode, boolean issued, String token, String note) {}
}
