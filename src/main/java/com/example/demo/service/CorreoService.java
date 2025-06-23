package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CorreoService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * Envío de un correo individual de texto plano o HTML simple
     */

    public void enviarCorreo(String to, String subject, String messageText) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("sabatrax23@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(messageText, true); // `true` si usas HTML

            mailSender.send(message);
            System.out.println("✅ Correo enviado a: " + to);

        } catch (MailException e) {
            System.err.println("❌ Error al enviar el correo: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Envío de correos masivos en segundo plano usando plantillas HTML
     */
    @Async
    public void enviarCorreoMasivoConPlantillaAsincrono(List<String> destinatarios,
            String asunto,
            String nombrePlantilla,
            String contenidoAdmin) {
        System.out.println("🟢 Servicio de correo masivo INVOCADO");
        System.out.println("📝 Asunto: " + asunto);
        System.out.println("📝 Plantilla: " + nombrePlantilla);
        System.out.println("📝 Mensaje: " + contenidoAdmin);

        if (destinatarios == null || destinatarios.isEmpty()) {
            System.out.println("⚠️ No hay destinatarios.");
            return;
        }

        int enviados = 0;
        int errores = 0;

        for (String email : destinatarios) {
            if (email == null || !email.contains("@")) {
                System.err.println("⚠️ Correo inválido: " + email);
                errores++;
                continue;
            }

            try {
                Context context = new Context();
                context.setVariable("asunto", asunto);
                context.setVariable("contenidoMensaje", contenidoAdmin);

                String cuerpoHtml = templateEngine.process(nombrePlantilla, context);

                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());

                helper.setFrom("sabatrax23@gmail.com");
                helper.setTo(email.trim());
                helper.setSubject(asunto);
                helper.setText(cuerpoHtml, true);

                mailSender.send(mensaje);
                enviados++;
                System.out.println("📤 Correo enviado a: " + email);

                Thread.sleep(150); // ❄️ Control de spam

            } catch (Exception e) {
                System.err.println("❌ Error al enviar a " + email + ": " + e.getMessage());
                errores++;
            }
        }

        System.out.println("✅ ENVÍO MASIVO FINALIZADO — Éxitos: " + enviados + " | Errores: " + errores);
    }
}
