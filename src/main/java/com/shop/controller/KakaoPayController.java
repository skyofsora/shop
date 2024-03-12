package com.shop.controller;

import com.shop.dto.PaymentResponseDto;
import com.shop.service.KakaoPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/kakaopay")
public class KakaoPayController {
    private final KakaoPaymentService kakaoPaymentService;

    @GetMapping("purchase/{order_id}")
    public @ResponseBody ResponseEntity<?> purchase(@PathVariable("order_id") Long orderId, Principal principal) {
        PaymentResponseDto dto = kakaoPaymentService.purchase(orderId, principal.getName());
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("success/{order_id}")
    public String success(@PathVariable("order_id") Long orderId, @RequestParam("pg_token") String pg_token) {
        if (kakaoPaymentService.paymentApprove(orderId, pg_token).getStatusCode() == HttpStatusCode.valueOf(200)) {
            return "purchase/success";
        }
        return "purchase/fail";
    }

    @GetMapping("/fail/{order_id}")
    public String fail(@PathVariable("order_id") Long orderId) {
        kakaoPaymentService.fail(orderId);
        return "purchase/fail";
    }

    @GetMapping("/cancel/{order_id}")
    public String cancel(@PathVariable("order_id") Long orderId) {
        kakaoPaymentService.cancel(orderId);
        return "purchase/fail";
    }

    @PostMapping("/refund/{order_id}")
    public @ResponseBody ResponseEntity<String> refund(@PathVariable("order_id") Long orderId) {
        return ResponseEntity.ok().body("환불이 완료되었습니다.");
    }
}
