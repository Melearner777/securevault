package com.jishan.securevault.service;

import com.jishan.securevault.entity.User;
import com.jishan.securevault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class TwoFactorService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a 6-digit OTP, saves it (expires in 5 minutes),
     * and sends it to the user's registered email.
     * Returns true if sent successfully, false if mail config is broken.
     */
    public boolean sendOtp(User user) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));

        // Store OTP and expiry BEFORE sending so it can still be verified manually
        user.setTwoFactorOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Send email — catch any SMTP/config errors gracefully
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("SecureVault - Your Login OTP");
            message.setText(
                "Hello " + user.getUsername() + ",\n\n" +
                "Your One-Time Password (OTP) for SecureVault login is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for 5 minutes.\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "--- SecureVault Security Team"
            );
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // Log the real error to console so developer can debug
            System.err.println("[2FA] Failed to send OTP email to " + user.getEmail() + ": " + e.getMessage());
            // Clear OTP so login doesn't hang in a broken state
            clearOtp(user);
            return false;
        }
    }

    /**
     * Verifies the OTP entered by the user.
     * Returns true if valid and not expired.
     */
    public boolean verifyOtp(User user, String inputOtp) {
        if (user.getTwoFactorOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            // OTP expired — clear it
            clearOtp(user);
            return false;
        }
        boolean match = user.getTwoFactorOtp().equals(inputOtp.trim());
        if (match) clearOtp(user);
        return match;
    }

    /** Clears OTP fields after use or expiry */
    public void clearOtp(User user) {
        user.setTwoFactorOtp(null);
        user.setOtpExpiry(null);
        user.setPendingAccessLevel(null);
        userRepository.save(user);
    }
}

