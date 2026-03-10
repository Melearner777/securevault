package com.jishan.securevault.service;

import com.jishan.securevault.entity.User;
import com.jishan.securevault.repository.UserRepository;
import com.jishan.securevault.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TwoFactorService twoFactorService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Register new user with two passwords + security question + optional email for 2FA
    public String register(String username, String password1, String password2,
                           String securityQuestion, String securityAnswer, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            return "Username already exists";
        }
        if (securityQuestion == null || securityQuestion.isBlank() ||
            securityAnswer   == null || securityAnswer.isBlank()) {
            return "Security question and answer are required";
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordLevel1(encoder.encode(password1));
        user.setPasswordLevel2(encoder.encode(password2));
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswer(encoder.encode(securityAnswer.trim().toLowerCase()));
        user.setFailedAttempts(0);
        user.setLocked(false);
        if (email != null && !email.isBlank()) {
            user.setEmail(email.trim().toLowerCase());
        }
        userRepository.save(user);
        return "User registered successfully";
    }

    // Get security question for a username (safe — does not expose answer)
    public String getSecurityQuestion(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "USER_NOT_FOUND";
        String question = opt.get().getSecurityQuestion();
        if (question == null || question.isBlank()) return "NO_SECURITY_QUESTION";
        return question;
    }

    // Reset password1 only — after verifying security answer
    public String resetPassword(String username, String securityAnswer, String newPassword1) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";

        User user = opt.get();

        if (!encoder.matches(securityAnswer.trim().toLowerCase(), user.getSecurityAnswer())) {
            return "Security answer is incorrect";
        }

        user.setPasswordLevel1(encoder.encode(newPassword1));
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
        return "Password reset successful. You can now login with your new password.";
    }

    // Login — checks level2 first, then level1
    // If 2FA enabled → sends OTP and returns "2FA_REQUIRED:<username>"
    public String login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (user.isLocked()) {
            return "Account is locked due to too many failed attempts";
        }

        String matchedLevel = null;

        // Level 2 → Secure Vault access
        if (encoder.matches(password, user.getPasswordLevel2())) {
            matchedLevel = "LEVEL2";
        }
        // Level 1 → Normal access
        else if (encoder.matches(password, user.getPasswordLevel1())) {
            matchedLevel = "LEVEL1";
        }

        if (matchedLevel == null) {
            handleFailedAttempt(user);
            return "Invalid credentials. Attempts left: " + (5 - user.getFailedAttempts());
        }

        resetFailedAttempts(user);

        // ── 2FA check ───────────────────────────────────────────
        if (user.isTwoFactorEnabled()) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                // 2FA enabled but no email — fallback to direct login
                return jwtUtil.generateToken(username, matchedLevel);
            }
            user.setPendingAccessLevel(matchedLevel);
            userRepository.save(user);
            boolean sent = twoFactorService.sendOtp(user);
            if (!sent) {
                return "EMAIL_ERROR: Failed to send OTP. Check server mail configuration.";
            }
            return "2FA_REQUIRED:" + username;
        }

        return jwtUtil.generateToken(username, matchedLevel);
    }

    // Verify OTP and issue JWT if correct
    public String verifyOtp(String username, String otp) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";

        User user = opt.get();
        String pendingLevel = user.getPendingAccessLevel();

        if (pendingLevel == null) {
            return "No pending 2FA session. Please login again.";
        }

        if (!twoFactorService.verifyOtp(user, otp)) {
            return "Invalid or expired OTP";
        }

        return jwtUtil.generateToken(username, pendingLevel);
    }

    // Enable 2FA — user must have an email set
    public String enable2FA(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";
        User user = opt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return "NO_EMAIL";
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        return "2FA enabled successfully";
    }

    // Disable 2FA
    public String disable2FA(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";
        User user = opt.get();
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        return "2FA disabled successfully";
    }

    // Update email for a logged-in user
    public String updateEmail(String username, String newEmail) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";
        User user = opt.get();
        user.setEmail(newEmail.trim().toLowerCase());
        userRepository.save(user);
        return "Email updated successfully";
    }

    // Get 2FA status
    public boolean is2FAEnabled(String username) {
        return userRepository.findByUsername(username)
                .map(User::isTwoFactorEnabled)
                .orElse(false);
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= 5) {
            user.setLocked(true);
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
    }
}

