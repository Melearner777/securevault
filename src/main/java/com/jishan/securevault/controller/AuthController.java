package com.jishan.securevault.controller;

import com.jishan.securevault.security.JwtUtil;
import com.jishan.securevault.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    // Body: { "username":"jishan","password1":"...","password2":"...","securityQuestion1":"...","securityAnswer1":"...","securityQuestion2":"...","securityAnswer2":"..." }
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        String result = authService.register(
            body.get("username"),
            body.get("password1"),
            body.get("password2"),
            body.get("securityQuestion1"),
            body.get("securityAnswer1"),
            body.get("securityQuestion2"),
            body.get("securityAnswer2")
        );
        return ResponseEntity.ok(result);
    }

    // GET /auth/security-questions?username=jishan
    @GetMapping("/security-questions")
    public ResponseEntity<Map<String, String>> getSecurityQuestions(@RequestParam String username) {
        Map<String, String> questions = authService.getSecurityQuestions(username);
        if (questions == null) {
            return ResponseEntity.status(404).body(Map.of("error", "USER_NOT_FOUND"));
        }
        return ResponseEntity.ok(questions);
    }

    // Backward-compatible single question endpoint for password1 recovery.
    @GetMapping("/security-question")
    public ResponseEntity<String> getSecurityQuestion(@RequestParam String username) {
        Map<String, String> questions = authService.getSecurityQuestions(username);
        if (questions == null) {
            return ResponseEntity.status(404).body("USER_NOT_FOUND");
        }
        String question = questions.get("securityQuestion1");
        if (question == null || question.isBlank()) {
            return ResponseEntity.status(400).body("NO_SECURITY_QUESTION");
        }
        return ResponseEntity.ok(question);
    }

    // GET /auth/password2-status?username=jishan
    @GetMapping("/password2-status")
    public ResponseEntity<String> password2Status(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        return ResponseEntity.ok(authService.getPassword2Status(username));
    }

    // POST /auth/setup-password2
    // Creates password2 after the user has already logged in with password1.
    @PostMapping("/setup-password2")
    public ResponseEntity<String> setupPassword2(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                 @RequestBody Map<String, String> body) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        String token = authHeader.substring(7);
        String accessLevel = jwtUtil.extractAccessLevel(token);
        if (!"LEVEL1".equals(accessLevel)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password 2 setup is allowed after password 1 login");
        }

        String result = authService.setupPassword2(username, body.get("password2"));
        if (result.startsWith("ey")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    // POST /auth/reset-password1
    @PostMapping("/reset-password1")
    public ResponseEntity<String> resetPassword1(@RequestBody Map<String, String> body) {
        String result = authService.resetPassword1(
            body.get("username"),
            body.get("securityAnswer"),
            body.get("newPassword")
        );
        if (result.toLowerCase().contains("successful")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    // POST /auth/reset-password2
    @PostMapping("/reset-password2")
    public ResponseEntity<String> resetPassword2(@RequestBody Map<String, String> body) {
        String result = authService.resetPassword2(
            body.get("username"),
            body.get("securityAnswer"),
            body.get("newPassword")
        );
        if (result.toLowerCase().contains("successful")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    // POST /auth/recover-by-question
    @PostMapping("/recover-by-question")
    public ResponseEntity<String> recoverByQuestion(@RequestBody Map<String, String> body) {
        String result = authService.recoverByQuestion(
            body.get("username"),
            body.get("questionKey"),
            body.get("securityAnswer"),
            body.get("newPassword")
        );
        if (result.toLowerCase().contains("successful")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    // Backward-compatible alias for password1 recovery.
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String result = authService.resetPassword(
            body.get("username"),
            body.get("securityAnswer"),
            body.get("newPassword")
        );
        if (result.toLowerCase().contains("successful")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    // POST /auth/login
    // Automatically detects the matching stored password and returns the correct JWT token.
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        String result = authService.login(body.get("username"), body.get("password"));
        return ResponseEntity.ok(result);
    }

    // POST /auth/login-password2
    // Direct Password 2 login from main page. Returns a LEVEL2 JWT token.
    @PostMapping("/login-password2")
    public ResponseEntity<String> loginWithPassword2(@RequestBody Map<String, String> body) {
        String result = authService.loginWithPassword2(body.get("username"), body.get("password"));
        return ResponseEntity.ok(result);
    }

    // POST /auth/unlock-secure
    // Requires a valid LEVEL1 token in Authorization header and the user's password 2.
    @PostMapping("/unlock-secure")
    public ResponseEntity<String> unlockSecureVault(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                    @RequestBody Map<String, String> body) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        String token = authHeader.substring(7);
        String accessLevel = jwtUtil.extractAccessLevel(token);
        if (!"LEVEL1".equals(accessLevel)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password 2 can only be used after password 1 login");
        }

        String result = authService.unlockSecureVault(username, body.get("password2"));
        if (result.startsWith("ey")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    private String extractUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.extractUsername(token);
    }
}
