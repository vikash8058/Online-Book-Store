package com.bookstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/*
 * EmailService — sends OTP email via Gmail SMTP.
 *
 * JavaMailSender → Spring's email sending interface.
 * Auto-configured by spring-boot-starter-mail dependency.
 * Uses Gmail SMTP settings from application.properties.
 *
 * SimpleMailMessage → plain text email (no HTML).
 * For HTML emails we would use MimeMessage instead.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    /*
     * JavaMailSender — Spring auto-creates this bean using
     * spring.mail.* properties in application.properties.
     * We just inject and use it.
     */
    private final JavaMailSender mailSender;

    /*
     * Reads spring.mail.username from application.properties.
     * Used as the FROM address in every email we send.
     */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /*
     * Builds and sends OTP email to the user.
     * Called by OtpService.generateAndSendOtp() after saving OTP to DB.
     *
     * SimpleMailMessage fields:
     *   setFrom()    → sender email (our Gmail)
     *   setTo()      → receiver email (user's email)
     *   setSubject() → email subject line
     *   setText()    → plain text body
     */
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your OTP for Online Book Store Registration");
        message.setText(
                "Hi,\n\n" +
                "Your OTP for registration is:\n\n" +
                "        " + otp + "\n\n" +
                "This OTP is valid for 5 minutes only.\n" +
                "Do not share this OTP with anyone.\n\n" +
                "Regards,\n" +
                "Online Book Store - Vikash Prajapati"
        );
        mailSender.send(message);
    }
        /*
         * sendPasswordResetOtpEmail() — UC11
         * Sends 6-digit OTP for password reset.
         *
         * Different subject and body from registration OTP
         * so user knows this is for password reset.
         * Same OTP mechanism — 6 digits, 5 min expiry.
         */
        public void sendPasswordResetOtpEmail(String toEmail, String otp) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your OTP for Online Book Store Password Reset");
            message.setText(
                    "Hi,\n\n" +
                    "You requested a password reset for your Book Store account.\n\n" +
                    "Your OTP for password reset is:\n\n" +
                    "        " + otp + "\n\n" +
                    "This OTP is valid for 5 minutes only.\n" +
                    "If you did not request this, please ignore this email.\n" +
                    "Do not share this OTP with anyone.\n\n" +
                    "Regards,\n" +
                    "Online Book Store - Vikash Prajapati"
            );
            mailSender.send(message);
        }
    
}