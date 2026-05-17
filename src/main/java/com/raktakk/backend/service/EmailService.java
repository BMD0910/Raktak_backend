package com.raktakk.backend.service;

import com.raktakk.backend.repository.SiteSettingsRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SiteSettingsRepository siteSettingsRepository;

    @Value("${mail.from:support@raktakk.com}")
    private String fromEmail;

    @Value("${mail.admin:admin@raktakk.com}")
    private String defaultSupportEmail;

    @Value("${app.name:Raktakk}")
    private String appName;

    /**
     * Récupère l'adresse support depuis les paramètres du site.
     * Le champ supportEmail configuré dans la page paramètres est la source de vérité.
     */
    private String getSupportEmail() {
        try {
            return siteSettingsRepository.findFirstByOrderByUpdatedAtDesc()
                    .map(settings -> settings.getSupportEmail())
                    .filter(email -> email != null && !email.isBlank())
                    .orElse(defaultSupportEmail);
        } catch (Exception e) {
            log.warn("⚠️ Impossible de récupérer l'email support depuis les paramètres: {}", e.getMessage());
            return defaultSupportEmail;
        }
    }

    public void sendContactNotificationToAdmin(String visitorName, String visitorEmail, String subject, String message, String phone) {
        try {
            String supportEmail = getSupportEmail();
            log.info("📧 Récupération email support: {}", supportEmail);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = buildContactEmailHtml(visitorName, visitorEmail, subject, message, phone);

            helper.setFrom(fromEmail);
            helper.setTo(supportEmail);
            helper.setSubject("📧 Nouveau message de contact: " + subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ Email de contact envoyé au support: {} (De: {})", supportEmail, visitorEmail);
        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi du mail au support: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer le message. Veuillez réessayer.");
        } catch (Exception e) {
            log.error("❌ Erreur générale d'envoi email: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    public void sendConfirmationToVisitor(String visitorName, String visitorEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(visitorEmail);
            message.setSubject("✅ Votre message a été reçu - " + appName);
            message.setText("Bonjour " + visitorName + ",\n\n" +
                    "Merci d'avoir pris contact avec nous. Nous avons bien reçu votre message.\n" +
                    "Notre équipe vous répondra dans les 24 heures ouvrables.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe " + appName);

            mailSender.send(message);
            log.info("✅ Email de confirmation envoyé à: {}", visitorEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de la confirmation: {}", e.getMessage());
            // Ne pas lancer d'exception ici - ce n'est pas bloquant
        }
    }

    private String buildContactEmailHtml(String visitorName, String visitorEmail, String subject, String message, String phone) {
        return """
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; color: #333; background-color: #f5f5f5; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 30px;">
                        <div style="border-left: 4px solid #FF5A1F; padding-left: 20px; margin-bottom: 20px;">
                            <h2 style="color: #FF5A1F; margin: 0;">📧 Nouveau Message de Contact</h2>
                        </div>

                        <div style="background-color: #f9f9f9; padding: 15px; border-radius: 6px; margin-bottom: 20px;">
                            <p style="margin: 8px 0;"><strong>De:</strong> {VISITOR_NAME}</p>
                            <p style="margin: 8px 0;"><strong>Email:</strong> <a href="mailto:{VISITOR_EMAIL}">{VISITOR_EMAIL}</a></p>
                            <p style="margin: 8px 0;"><strong>Téléphone:</strong> {PHONE}</p>
                            <p style="margin: 8px 0;"><strong>Sujet:</strong> {SUBJECT}</p>
                        </div>

                        <div style="border-top: 1px solid #ddd; padding-top: 20px; margin-bottom: 20px;">
                            <h3 style="color: #333; margin-top: 0;">Message:</h3>
                            <p style="white-space: pre-wrap; line-height: 1.6; background-color: #f9f9f9; padding: 15px; border-radius: 6px;">{MESSAGE}</p>
                        </div>

                        <div style="background-color: #fafafa; padding: 15px; border-radius: 6px; text-align: center;">
                            <p style="margin: 0; font-size: 14px; color: #666;">
                                <strong>Répondez rapidement à:</strong><br>
                                <a href="mailto:{VISITOR_EMAIL}" style="color: #FF5A1F; text-decoration: none; font-weight: bold;">Cliquez ici pour répondre</a>
                            </p>
                        </div>

                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; font-size: 12px; color: #999;">
                            <p>© Raktakk - Message automatique</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .replace("{VISITOR_NAME}", visitorName)
                .replace("{VISITOR_EMAIL}", visitorEmail)
                .replace("{PHONE}", phone != null ? phone : "Non fourni")
                .replace("{SUBJECT}", subject)
                .replace("{MESSAGE}", message);
    }
}
