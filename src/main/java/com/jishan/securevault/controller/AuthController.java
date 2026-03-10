package com.jishan.securevault.controller;

import com.jishan.securevault.security.JwtUtil;
import com.jishan.securevault.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    // POST /auth/register
    // Body: { "username":"jishan","password1":"...","password2":"...","securityQuestion":"...","securityAnswer":"...","email":"..." }
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        String result = authService.register(
            body.get("username"),
            body.get("password1"),
            body.get("password2"),
            body.get("securityQuestion"),
            body.get("securityAnswer"),
            body.get("email")          // optional — needed for 2FA
        );
        return ResponseEntity.ok(result);
    }

    // GET /auth/security-question?username=jishan
    @GetMapping("/security-question")
    public ResponseEntity<String> getSecurityQuestion(@RequestParam String username) {
        String question = authService.getSecurityQuestion(username);
        if ("USER_NOT_FOUND".equals(question))
            return ResponseEntity.status(404).body("USER_NOT_FOUND");
        if ("NO_SECURITY_QUESTION".equals(question))
            return ResponseEntity.status(400).body("NO_SECURITY_QUESTION");
        return ResponseEntity.ok(question);
    }

    // POST /auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String result = authService.resetPassword(
            body.get("username"),
            body.get("securityAnswer"),
            body.get("newPassword")
        );
        return ResponseEntity.ok(result);
    }

    // POST /auth/login
    // Returns: JWT token OR "2FA_REQUIRED:<username>"
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        String result = authService.login(body.get("username"), body.get("password"));
        return ResponseEntity.ok(result);
    }

    // POST /auth/verify-otp
    // Body: { "username":"jishan", "otp":"123456" }
    // Returns JWT token on success
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> body) {
        String result = authService.verifyOtp(body.get("username"), body.get("otp"));
        return ResponseEntity.ok(result);
    }

    // POST /auth/enable-2fa   (requires JWT)
    @PostMapping("/enable-2fa")
    public ResponseEntity<String> enable2FA(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(authService.enable2FA(username));
    }

    // POST /auth/disable-2fa  (requires JWT)
    @PostMapping("/disable-2fa")
    public ResponseEntity<String> disable2FA(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(authService.disable2FA(username));
    }

    // POST /auth/update-email  (requires JWT)
    // Body: { "email": "jishan@gmail.com" }
    @PostMapping("/update-email")
    public ResponseEntity<String> updateEmail(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody Map<String, String> body) {
        String username = extractUsername(authHeader);
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(authService.updateEmail(username, body.get("email")));
    }

    // GET /auth/2fa-status  (requires JWT)
    @GetMapping("/2fa-status")
    public ResponseEntity<Boolean> get2FAStatus(@RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authService.is2FAEnabled(username));
    }

    private String extractUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.extractUsername(token);
    }
}


