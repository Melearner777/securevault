package com.jishan.securevault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordLevel1;

    @Column(nullable = false)
    private String passwordLevel2;

    private int failedAttempts;
    private boolean locked;

    @Column
    private String securityQuestion;

    @Column
    private String securityAnswer; // stored as BCrypt hash

    // ── 2FA fields ──────────────────────────────────────
    @Column(unique = true)
    private String email;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean twoFactorEnabled = false;

    @Column
    private String twoFactorOtp;          // 6-digit OTP (plain, short-lived)

    @Column
    private LocalDateTime otpExpiry;      // OTP valid for 5 minutes

    @Column
    private String pendingAccessLevel;    // LEVEL1 or LEVEL2 — stored while OTP pending

    // ── Getters & Setters ────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordLevel1() { return passwordLevel1; }
    public void setPasswordLevel1(String passwordLevel1) { this.passwordLevel1 = passwordLevel1; }

    public String getPasswordLevel2() { return passwordLevel2; }
    public void setPasswordLevel2(String passwordLevel2) { this.passwordLevel2 = passwordLevel2; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getTwoFactorOtp() { return twoFactorOtp; }
    public void setTwoFactorOtp(String twoFactorOtp) { this.twoFactorOtp = twoFactorOtp; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    public String getPendingAccessLevel() { return pendingAccessLevel; }
    public void setPendingAccessLevel(String pendingAccessLevel) { this.pendingAccessLevel = pendingAccessLevel; }
}
