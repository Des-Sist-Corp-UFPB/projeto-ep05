package br.ufpb.dsc.mercado.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@delightssweet.com}")
    private String remetente;

    // URL base do frontend, usada para montar o link de redefinição de senha.
    // Ex.: https://eq05.dsc.rodrigor.com
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEmailRecuperacaoSenha(String destinatario, String token) {
        String link = frontendUrl + "/redefinir-senha?token=" + token;

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Recuperação de senha - Delights Sweet");
        mensagem.setText(
                "Olá!\n\n" +
                "Recebemos uma solicitação para redefinir sua senha.\n\n" +
                "Use o token abaixo (ou clique no link) para criar uma nova senha. " +
                "Ele expira em 30 minutos:\n\n" +
                "Token: " + token + "\n" +
                "Link: " + link + "\n\n" +
                "Se você não solicitou isso, pode ignorar este e-mail com segurança."
        );

        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            // Não relançamos para o controller: a resposta da API de recuperação
            // de senha precisa continuar genérica (não revelar se o e-mail existe
            // ou se o envio falhou). Mas registramos o erro para diagnóstico.
            log.error("Falha ao enviar e-mail de recuperação de senha", e);
        }
    }
}
