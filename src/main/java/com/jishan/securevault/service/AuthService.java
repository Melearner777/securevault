package com.jishan.securevault.service;

import com.jishan.securevault.entity.User;
import com.jishan.securevault.repository.UserRepository;
import com.jishan.securevault.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Register new user with password 1, password 2, and two security questions.
    public String register(String username, String password1,
                           String password2,
                           String securityQuestion1, String securityAnswer1,
                           String securityQuestion2, String securityAnswer2) {
        if (username == null || username.isBlank() ||
            password1 == null || password1.isBlank() ||
            password2 == null || password2.isBlank()) {
            return "Username and password1 are required";
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return "Username already exists";
        }
        if (securityQuestion1 == null || securityQuestion1.isBlank() ||
            securityAnswer1   == null || securityAnswer1.isBlank() ||
            securityQuestion2 == null || securityQuestion2.isBlank() ||
            securityAnswer2   == null || securityAnswer2.isBlank()) {
            return "Both security questions and answers are required";
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordLevel1(encoder.encode(password1));
        user.setPasswordLevel2(encoder.encode(password2));
        user.setSecurityQuestion(securityQuestion1);
        user.setSecurityAnswer(encoder.encode(securityAnswer1.trim().toLowerCase()));
        user.setSecurityQuestion1(securityQuestion1);
        user.setSecurityAnswer1(encoder.encode(securityAnswer1.trim().toLowerCase()));
        user.setSecurityQuestion2(securityQuestion2);
        user.setSecurityAnswer2(encoder.encode(securityAnswer2.trim().toLowerCase()));
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
        return "User registered successfully";
    }

    // Get both security questions for a username (safe — does not expose answers)
    public Map<String, String> getSecurityQuestions(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return null;

        User user = opt.get();
        Map<String, String> questions = new HashMap<>();
        questions.put("securityQuestion1", firstNonBlank(user.getSecurityQuestion1(), user.getSecurityQuestion()));
        questions.put("securityQuestion2", firstNonBlank(user.getSecurityQuestion2(), user.getSecurityQuestion()));
        return questions;
    }

    // Return whether password2 already exists for this user.
    public String getPassword2Status(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "USER_NOT_FOUND";
        return (opt.get().getPasswordLevel2() == null || opt.get().getPasswordLevel2().isBlank())
                ? "NOT_SET"
                : "SET";
    }

    // Set password2 after password1 login.
    public String setupPassword2(String username, String password2) {
        if (username == null || username.isBlank() || password2 == null || password2.isBlank()) {
            return "Username and password2 are required";
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";

        User user = opt.get();
        if (user.getPasswordLevel2() != null && !user.getPasswordLevel2().isBlank()) {
            return "Password 2 already exists";
        }

        user.setPasswordLevel2(encoder.encode(password2));
        userRepository.save(user);
        return jwtUtil.generateToken(username, "LEVEL2");
    }

    // Reset password1 only — after verifying security answer 1
    public String resetPassword(String username, String securityAnswer, String newPassword1) {
        return resetPassword1(username, securityAnswer, newPassword1);
    }

    // Reset password1 after verifying security answer 1.
    public String resetPassword1(String username, String securityAnswer, String newPassword1) {
        if (username == null || username.isBlank() ||
            securityAnswer == null || securityAnswer.isBlank() ||
            newPassword1 == null || newPassword1.isBlank()) {
            return "Username, security answer, and new password are required";
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";

        User user = opt.get();

        String storedAnswer = firstNonBlank(user.getSecurityAnswer1(), user.getSecurityAnswer());
        if (storedAnswer == null || !encoder.matches(securityAnswer.trim().toLowerCase(), storedAnswer)) {
            return "Security answer is incorrect";
        }

        user.setPasswordLevel1(encoder.encode(newPassword1));
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
        return "Password reset successful. You can now login with your new password.";
    }

    // Reset password2 after verifying security answer 2.
    public String resetPassword2(String username, String securityAnswer, String newPassword2) {
        if (username == null || username.isBlank() ||
            securityAnswer == null || securityAnswer.isBlank() ||
            newPassword2 == null || newPassword2.isBlank()) {
            return "Username, security answer, and new password are required";
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";

        User user = opt.get();

        String storedAnswer = firstNonBlank(user.getSecurityAnswer2(), user.getSecurityAnswer());
        if (storedAnswer == null || !encoder.matches(securityAnswer.trim().toLowerCase(), storedAnswer)) {
            return "Security answer is incorrect";
        }

        user.setPasswordLevel2(encoder.encode(newPassword2));
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
        return "Password reset successful. You can now login with your new password.";
    }

    // Recover the matching stored password based on the selected security question.
    public String recoverByQuestion(String username, String questionKey, String securityAnswer, String newPassword) {
        if (username == null || username.isBlank() ||
            questionKey == null || questionKey.isBlank() ||
            securityAnswer == null || securityAnswer.isBlank() ||
            newPassword == null || newPassword.isBlank()) {
            return "Username, security question, security answer, and new password are required";
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return "User not found";

        User user = opt.get();
        String key = questionKey.trim();
        String question = null;
        String storedAnswer = null;
        boolean resetLevel1 = false;

        if ("securityQuestion1".equals(key)) {
            question = firstNonBlank(user.getSecurityQuestion1(), user.getSecurityQuestion());
            storedAnswer = firstNonBlank(user.getSecurityAnswer1(), user.getSecurityAnswer());
            resetLevel1 = true;
        } else if ("securityQuestion2".equals(key)) {
            question = firstNonBlank(user.getSecurityQuestion2(), user.getSecurityQuestion());
            storedAnswer = firstNonBlank(user.getSecurityAnswer2(), user.getSecurityAnswer());
            resetLevel1 = false;
        } else {
            return "Invalid security question selection";
        }

        if (question == null || storedAnswer == null) {
            return "Security question is not set for this account";
        }

        if (!encoder.matches(securityAnswer.trim().toLowerCase(), storedAnswer)) {
            return "Security answer is incorrect";
        }

        if (resetLevel1) {
            user.setPasswordLevel1(encoder.encode(newPassword));
        } else {
            user.setPasswordLevel2(encoder.encode(newPassword));
        }

        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);
        return "Password reset successful. You can now login with your new password.";
    }

    // Login — automatically detects which stored password matches and returns the correct token.
    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return "Username and password are required";
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (user.isLocked()) {
            return "Account is locked due to too many failed attempts";
        }

        if (user.getPasswordLevel2() != null && !user.getPasswordLevel2().isBlank() && encoder.matches(password, user.getPasswordLevel2())) {
            resetFailedAttempts(user);
            return jwtUtil.generateToken(username, "LEVEL2");
        }

        if (encoder.matches(password, user.getPasswordLevel1())) {
            resetFailedAttempts(user);
            return jwtUtil.generateToken(username, "LEVEL1");
        }

        handleFailedAttempt(user);
        return "Invalid credentials. Attempts left: " + (5 - user.getFailedAttempts());
    }


    // Password 2 unlock — for users who already created password2 after login.
    public String unlockSecureVault(String username, String password2) {
        if (username == null || username.isBlank() || password2 == null || password2.isBlank()) {
            return "Username and password2 are required";
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();

        if (user.isLocked()) {
            return "Account is locked due to too many failed attempts";
        }

        if (user.getPasswordLevel2() == null || user.getPasswordLevel2().isBlank()) {
            return "Password 2 is not set yet";
        }

        if (!encoder.matches(password2, user.getPasswordLevel2())) {
            handleFailedAttempt(user);
            return "Invalid secure password. Attempts left: " + (5 - user.getFailedAttempts());
        }

        resetFailedAttempts(user);
        return jwtUtil.generateToken(username, "LEVEL2");
    }

    // Password 2 direct login from main login page — returns a LEVEL2 token.
    public String loginWithPassword2(String username, String password2) {
        if (username == null || username.isBlank() || password2 == null || password2.isBlank()) {
            return "Username and password2 are required";
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return "User not found";
        }

        User user = optionalUser.get();
        if (user.isLocked()) {
            return "Account is locked due to too many failed attempts";
        }

        if (user.getPasswordLevel2() == null || user.getPasswordLevel2().isBlank()) {
            return "Password 2 is not set yet";
        }

        if (!encoder.matches(password2, user.getPasswordLevel2())) {
            handleFailedAttempt(user);
            return "Invalid Password 2. Attempts left: " + (5 - user.getFailedAttempts());
        }

        resetFailedAttempts(user);
        return jwtUtil.generateToken(username, "LEVEL2");
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

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        if (fallback != null && !fallback.isBlank()) return fallback;
        return null;
    }
}
