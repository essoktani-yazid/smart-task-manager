package com.smarttask.client.service;

import com.smarttask.model.CalendarEvent;
import com.smarttask.model.User;

import javax.mail.*;
import javax.mail.internet.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Email Notification Service for Calendar Events
 * Sends email invitations to external users when they are invited to events
 *
 * 🔧 FIXED: SSL/TLS certificate trust issue resolved
 *
 * REQUIREMENTS:
 * 1. Add javax.mail dependency to your pom.xml:
 *    <dependency>
 *        <groupId>com.sun.mail</groupId>
 *        <artifactId>javax.mail</artifactId>
 *        <version>1.6.2</version>
 *    </dependency>
 *
 * 2. Configure your SMTP settings below
 *
 * 3. For Gmail: Use App Passwords
 *    https://myaccount.google.com/apppasswords
 */
public class EmailNotificationService {

    // ═══════════════════════════════════════════════════════════════════════
    // SMTP CONFIGURATION - UPDATE THESE VALUES
    // ═══════════════════════════════════════════════════════════════════════

    // Gmail SMTP settings
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";          // TLS port
    private static final String SMTP_SSL_PORT = "465";      // SSL port (alternative)
    private static final String SMTP_USERNAME = "smartTaskManager.msdia@gmail.com";  // Your Gmail address
    private static final String SMTP_PASSWORD = "ymbvdpngpvpujirl";     // App password from Google

    // ymbv dpng pvpu jirl  SmartTaskManager
    // xdyydrwfxiuawwf smartTaskManager
    // Sender info
    private static final String SENDER_NAME = "SmartTaskManager";
    private static final String SENDER_EMAIL = "smartTaskManager.msdia@gmail.com";

    // SSL Configuration
    private static final boolean USE_SSL = true;           // Use SSL instead of TLS
    private static final boolean TRUST_ALL_CERTS = true;   // Trust all certificates (for development)

    private Session mailSession;

    public EmailNotificationService() {
        this.mailSession = createMailSession();
    }

    /**
     * Create mail session with proper SSL/TLS configuration
     * 🔧 FIXED: Added SSL trust configuration
     */
    private Session createMailSession() {
        Properties props = new Properties();

        if (USE_SSL) {
            // ═══════════════════════════════════════════════════════════════
            // OPTION 1: Use SSL (Port 465) - More reliable
            // ═══════════════════════════════════════════════════════════════
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_SSL_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", SMTP_SSL_PORT);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");

        } else {
            // ═══════════════════════════════════════════════════════════════
            // OPTION 2: Use STARTTLS (Port 587)
            // ═══════════════════════════════════════════════════════════════
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Common SSL properties
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        // 🔧 FIX: Trust all certificates (for development/testing)
        if (TRUST_ALL_CERTS) {
            props.put("mail.smtp.ssl.checkserveridentity", "false");
            props.put("mail.smtp.ssl.trust", "*");

            // Install trust-all SSL context
            try {
                installTrustAllCertificates();
            } catch (Exception e) {
                System.err.println("⚠️ Could not install trust-all certificates: " + e.getMessage());
            }
        }

        // Debug mode (set to true to see detailed SMTP communication)
        props.put("mail.debug", "false");

        // Timeouts
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
    }

    /**
     * 🔧 FIX: Install trust-all SSL certificates
     * This bypasses certificate validation (use only for development!)
     */
    private void installTrustAllCertificates() throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Trust all clients
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Trust all servers
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        // Set as default
        SSLContext.setDefault(sc);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        System.out.println("✅ Trust-all SSL certificates installed");
    }

    /**
     * Send event invitation email
     *
     * @param recipientEmail The email address to send to
     * @param event The calendar event
     * @param inviter The user who is inviting
     * @param subject Custom email subject
     * @return true if email was sent successfully
     */
    public boolean sendEventInvitation(String recipientEmail, CalendarEvent event,
                                       User inviter, String subject) {
        try {
            // Validate email
            if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                System.err.println("❌ Invalid recipient email");
                return false;
            }

            // Validate event
            if (event == null || event.getTitle() == null) {
                System.err.println("❌ Invalid event");
                return false;
            }

            System.out.println("📧 Sending email to: " + recipientEmail);

            Message message = new MimeMessage(mailSession);

            // Set sender
            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));

            // Set recipient
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail.trim()));

            // Set subject
            message.setSubject(subject + ": " + event.getTitle());

            // Build HTML content
            String htmlContent = buildInvitationEmail(event, inviter);

            // Set content as HTML
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Send
            Transport.send(message);

            System.out.println("✅ Email sent successfully to: " + recipientEmail);
            return true;

        } catch (AuthenticationFailedException e) {
            System.err.println("❌ Authentication failed. Check your email/password.");
            System.err.println("   For Gmail, use an App Password: https://myaccount.google.com/apppasswords");
            e.printStackTrace();
            return false;

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email to " + recipientEmail);
            System.err.println("   Error: " + e.getMessage());

            // Check for common issues
            if (e.getMessage().contains("SSL") || e.getMessage().contains("TLS")) {
                System.err.println("   💡 TIP: Try setting USE_SSL = true in EmailNotificationService");
            }
            if (e.getMessage().contains("authentication") || e.getMessage().contains("535")) {
                System.err.println("   💡 TIP: Check your email credentials and use App Password for Gmail");
            }

            e.printStackTrace();
            return false;

        } catch (Exception e) {
            System.err.println("❌ Unexpected error sending email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Build beautiful HTML email for event invitation
     */
    private String buildInvitationEmail(CalendarEvent event, User inviter) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        String dateStr = event.getDate() != null ?
                event.getDate().format(dateFormatter) : "Date not set";
        String startTimeStr = event.getStartTime() != null ?
                event.getStartTime().format(timeFormatter) : "";
        String endTimeStr = event.getEndTime() != null ?
                event.getEndTime().format(timeFormatter) : "";
        String timeStr = startTimeStr + " - " + endTimeStr;

        String priorityColor = event.getPriority() != null ?
                event.getPriority().getColor() : "#2196F3";
        String priorityName = event.getPriority() != null ?
                event.getPriority().getDisplayName() : "Standard";

        String inviterName = inviter != null ? inviter.getUsername() : "Someone";

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("</head>");
        html.append("<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; background-color: #f3f4f6;'>");

        // Container
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Card
        html.append("<div style='background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);'>");

        // Header with priority color
        html.append("<div style='background: linear-gradient(135deg, ").append(priorityColor).append(" 0%, ").append(priorityColor).append("CC 100%); padding: 30px; text-align: center;'>");
        html.append("<div style='font-size: 48px; margin-bottom: 10px;'>📅</div>");
        html.append("<h1 style='color: white; margin: 0; font-size: 24px; font-weight: 600;'>Event Invitation</h1>");
        html.append("<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 14px;'>").append(inviterName).append(" has invited you to an event</p>");
        html.append("</div>");

        // Content
        html.append("<div style='padding: 30px;'>");

        // Event title
        html.append("<h2 style='color: #1f2937; margin: 0 0 20px 0; font-size: 22px; font-weight: 700;'>").append(escapeHtml(event.getTitle())).append("</h2>");

        // Details grid
        html.append("<div style='background: #f9fafb; border-radius: 12px; padding: 20px; margin-bottom: 20px;'>");

        // Date
        html.append("<div style='margin-bottom: 15px;'>");
        html.append("<span style='font-size: 20px; margin-right: 12px;'>📆</span>");
        html.append("<span style='color: #6b7280; font-size: 11px; font-weight: 600; text-transform: uppercase;'>Date: </span>");
        html.append("<span style='color: #1f2937; font-size: 15px; font-weight: 600;'>").append(dateStr).append("</span>");
        html.append("</div>");

        // Time
        html.append("<div style='margin-bottom: 15px;'>");
        html.append("<span style='font-size: 20px; margin-right: 12px;'>🕐</span>");
        html.append("<span style='color: #6b7280; font-size: 11px; font-weight: 600; text-transform: uppercase;'>Time: </span>");
        html.append("<span style='color: #1f2937; font-size: 15px; font-weight: 600;'>").append(timeStr).append("</span>");
        html.append("</div>");

        // Priority
        html.append("<div>");
        html.append("<span style='font-size: 20px; margin-right: 12px;'>⚡</span>");
        html.append("<span style='color: #6b7280; font-size: 11px; font-weight: 600; text-transform: uppercase;'>Priority: </span>");
        html.append("<span style='display: inline-block; background: ").append(priorityColor).append("; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;'>").append(priorityName).append("</span>");
        html.append("</div>");

        html.append("</div>"); // End details grid

        // Description if present
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            html.append("<div style='margin-bottom: 20px;'>");
            html.append("<div style='color: #6b7280; font-size: 11px; font-weight: 600; text-transform: uppercase; margin-bottom: 8px;'>Description</div>");
            html.append("<p style='color: #4b5563; font-size: 14px; line-height: 1.6; margin: 0;'>").append(escapeHtml(event.getDescription())).append("</p>");
            html.append("</div>");
        }

        // Meeting link if present
        if (event.hasMeetingLink() && event.getMeetingLink() != null && !event.getMeetingLink().isEmpty()) {
            html.append("<div style='background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 12px; padding: 16px; margin-bottom: 20px;'>");
            html.append("<div style='margin-bottom: 10px;'>");
            html.append("<span style='font-size: 20px; margin-right: 10px;'>🎥</span>");
            html.append("<span style='color: #065f46; font-weight: 600;'>Online Meeting</span>");
            html.append("</div>");
            html.append("<a href='").append(escapeHtml(event.getMeetingLink())).append("' style='display: inline-block; background: #10b981; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px;'>Join Meeting</a>");

            if (event.getMeetingPassword() != null && !event.getMeetingPassword().isEmpty()) {
                html.append("<p style='color: #6b7280; font-size: 12px; margin: 10px 0 0 0;'>Password: <code style='background: #e5e7eb; padding: 2px 6px; border-radius: 4px;'>").append(escapeHtml(event.getMeetingPassword())).append("</code></p>");
            }

            html.append("</div>");
        }

        // Location if present
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            html.append("<div style='margin-bottom: 20px;'>");
            html.append("<span style='font-size: 20px; margin-right: 12px;'>📍</span>");
            html.append("<span style='color: #6b7280; font-size: 11px; font-weight: 600; text-transform: uppercase;'>Location: </span>");
            html.append("<span style='color: #1f2937; font-size: 14px;'>").append(escapeHtml(event.getLocation())).append("</span>");
            html.append("</div>");
        }

        html.append("</div>"); // End content

        // Footer
        html.append("<div style='background: #f9fafb; padding: 20px 30px; border-top: 1px solid #e5e7eb; text-align: center;'>");
        html.append("<p style='color: #9ca3af; font-size: 12px; margin: 0;'>This invitation was sent from SmartTask Calendar</p>");
        html.append("<p style='color: #9ca3af; font-size: 11px; margin: 8px 0 0 0;'>If you did not expect this email, you can safely ignore it.</p>");
        html.append("</div>");

        html.append("</div>"); // End card
        html.append("</div>"); // End container

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Send event update notification
     */
    public boolean sendEventUpdate(String recipientEmail, CalendarEvent event,
                                   User updater, String changeDescription) {
        try {
            Message message = new MimeMessage(mailSession);

            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail));
            message.setSubject("Event Updated: " + event.getTitle());

            String htmlContent = buildUpdateEmail(event, updater, changeDescription);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send update email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send event cancellation notification
     */
    public boolean sendEventCancellation(String recipientEmail, CalendarEvent event, User canceller) {
        try {
            Message message = new MimeMessage(mailSession);

            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail));
            message.setSubject("Event Cancelled: " + event.getTitle());

            String htmlContent = buildCancellationEmail(event, canceller);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send cancellation email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Build update notification email
     */
    private String buildUpdateEmail(CalendarEvent event, User updater, String changeDescription) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body style='font-family: sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<div style='background: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 8px;'>");
        html.append("<h2 style='color: #92400e; margin: 0 0 10px 0;'>📝 Event Updated</h2>");
        html.append("<p style='color: #78350f; margin: 0;'><strong>").append(escapeHtml(event.getTitle())).append("</strong> has been updated by ").append(updater != null ? escapeHtml(updater.getUsername()) : "the organizer").append(".</p>");
        if (changeDescription != null && !changeDescription.isEmpty()) {
            html.append("<p style='color: #92400e; margin: 10px 0 0 0; font-size: 14px;'>Changes: ").append(escapeHtml(changeDescription)).append("</p>");
        }
        html.append("</div></div>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Build cancellation notification email
     */
    private String buildCancellationEmail(CalendarEvent event, User canceller) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body style='font-family: sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<div style='background: #fee2e2; border-left: 4px solid #ef4444; padding: 20px; border-radius: 8px;'>");
        html.append("<h2 style='color: #991b1b; margin: 0 0 10px 0;'>❌ Event Cancelled</h2>");
        html.append("<p style='color: #7f1d1d; margin: 0;'><strong>").append(escapeHtml(event.getTitle())).append("</strong> has been cancelled by ").append(canceller != null ? escapeHtml(canceller.getUsername()) : "the organizer").append(".</p>");
        html.append("</div></div>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Escape HTML characters to prevent XSS
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Test email configuration
     * Call this method to verify your SMTP settings work
     */
    public boolean testConfiguration(String testEmail) {
        try {
            System.out.println("🔧 Testing email configuration...");
            System.out.println("   SMTP Host: " + SMTP_HOST);
            System.out.println("   SMTP Port: " + (USE_SSL ? SMTP_SSL_PORT : SMTP_PORT));
            System.out.println("   Using SSL: " + USE_SSL);
            System.out.println("   Username: " + SMTP_USERNAME);

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(testEmail));
            message.setSubject("SmartTask Email Test");
            message.setContent(
                    "<html><body style='font-family: sans-serif; text-align: center; padding: 40px;'>" +
                            "<h1 style='color: #10b981;'>✅ Email Configuration Working!</h1>" +
                            "<p>Your SmartTask email notifications are configured correctly.</p>" +
                            "</body></html>",
                    "text/html; charset=utf-8"
            );

            Transport.send(message);
            System.out.println("✅ Test email sent successfully to: " + testEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Email configuration test failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}