package com.shop.service;

import com.shop.entity.Email;
import com.shop.repository.EmailRepository;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final String senderEmail = "shop21450@gmail.com";
    private final JavaMailSender javaMailSender;
    private final EmailRepository emailRepository;

    public void sendOauthCodeEmail(String email) {
        MimeMessage message = javaMailSender.createMimeMessage();
        int code = (int) (Math.random() * 99999 + 100000);
        try {
            emailRepository.deleteByEmail(email);
        } catch (Exception ignored) {
        }
        emailRepository.save(new Email(email, code));
        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, emailNormalization(email));
            message.setSubject("이메일 인증");
            String body = "";
            body += "<h3>" + "요청하신 인증 번호입니다." + "</h3>";
            body += "<h1>" + code + "</h1>";
            body += "<h3>" + "감사합니다." + "</h3>";
            message.setText(body, "UTF-8", "html");
        } catch (MessagingException e) {
            e.fillInStackTrace();
        }
        javaMailSender.send(message);
    }

    public void sendOrderEmail(String email, String purchase) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            message.setFrom(senderEmail);
            message.setRecipients(Message.RecipientType.TO, emailNormalization(email));
            message.setSubject("[SHOP] 구매 영수증");
            String body = "<h2> 구매에 감사드립니다. </h2>" + "<h3> 귀하가 구매하신 품목의 영수증입니다.</h3>" + purchase + "<h3> 감사합니다. </h3>";
            message.setText(body, "UTF-8", "html");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        javaMailSender.send(message);
    }

    public boolean checkEmailAndCode(Email email) {
        Email temp = emailRepository.findByEmail(email.getEmail());
        return Objects.equals(temp.getNumber(), email.getNumber());
    }

    public String emailNormalization(String email) {
        Pattern pattern = Pattern.compile("\\([^)]*\\)");
        Matcher matcher = pattern.matcher(email);
        return matcher.replaceAll("");
    }
}
