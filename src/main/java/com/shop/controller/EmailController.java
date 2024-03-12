package com.shop.controller;

import com.shop.dto.SessionUser;
import com.shop.entity.Email;
import com.shop.service.EmailService;
import com.shop.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mail")
public class EmailController {
    private final EmailService emailService;
    private final MemberService memberService;
    private final HttpSession httpSession;

    @PostMapping("/sendmail")
    public ResponseEntity<String> emailSend(String email) {
        try {
            memberService.validateDuplicateMember(email); // 사용중인 이메일 체크
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        try {
            emailService.sendOauthCodeEmail(email);
        } catch (Exception e) {
            return new ResponseEntity<>("메일을 전송하지 못했습니다.", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("인증코드를 메일로 전송했습니다.", HttpStatus.OK);
    }

    @PostMapping("/sendmail-check")
    public ResponseEntity<String> checkMail(String email, int number) {
        if (emailService.checkEmailAndCode(new Email(email, number))) {
            httpSession.setAttribute("email", new SessionUser(email));
            return new ResponseEntity<>("인증번호 확인이 완료되었습니다.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("인증번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
