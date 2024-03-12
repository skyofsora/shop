package com.shop.service;

import com.shop.constant.OrderStatus;
import com.shop.dto.KakaoApproveResponseDto;
import com.shop.dto.KakaoPaymentCancelResponseDTO;
import com.shop.dto.PaymentResponseDto;
import com.shop.entity.Member;
import com.shop.entity.Order;
import com.shop.entity.OrderItem;
import com.shop.entity.PaymentPending;
import com.shop.repository.MemberRepository;
import com.shop.repository.OrderRepository;
import com.shop.repository.PaymentPendingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class KakaoPaymentService {
    private final String cid = "TC0ONETIME";
    @Autowired
    private PaymentPendingRepository paymentPendingRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Value("${KAKAO_ADMIN_KEY}")
    private String KAKAO_ADMIN_KEY;
    @Autowired
    private EmailService emailService;

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        String auth = "KakaoAK " + KAKAO_ADMIN_KEY;
        httpHeaders.set("Authorization", auth);
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return httpHeaders;
    }

    @Transactional
    public ResponseEntity<?> paymentApprove(Long orderId, String pg_token) {
        PaymentPending paymentPending = paymentPendingRepository.findByOrderId(orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        order.setOrderStatus(OrderStatus.ORDER);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("cid", cid);
        parameters.add("tid", paymentPending.getTid());
        parameters.add("partner_order_id", orderId + "");
        parameters.add("partner_user_id", paymentPending.getMember().getEmail());
        parameters.add("pg_token", pg_token);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, this.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject("https://kapi.kakao.com/v1/payment/approve", requestEntity, KakaoApproveResponseDto.class);
        sendEmail(order);
        return ResponseEntity.ok().body("결제에 성공했습니다.");
    }

    public PaymentResponseDto purchase(Long orderId, String email) {
        PaymentPending paymentPending = paymentPendingRepository.findByOrderId(orderId);
        paymentPendingRepository.delete(paymentPending);
        Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        Member member = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        return purchase(order, member);
    }

    @Transactional
    public PaymentResponseDto purchase(Order order, Member member) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        List<OrderItem> orderItems = order.getOrderItems();
        String item_name = orderItems.getFirst().getItem().getItemNm();
        if (orderItems.size() != 1) {
            item_name += "외 " + (orderItems.size() - 1) + "개 상품";
        }
        long total_amount = 0;
        for (OrderItem orderItem : orderItems) {
            total_amount += orderItem.getTotalPrice();
        }
        String order_id = order.getId() + "";
        String user_id = order.getCreatedBy();
        params.add("cid", cid);
        params.add("partner_order_id", order_id);
        params.add("partner_user_id", user_id);
        params.add("item_name", item_name);
        params.add("quantity", orderItems.size() + "");
        params.add("total_amount", total_amount + "");
        params.add("tax_free_amount", "0");
        params.add("approval_url", "http://3.36.36.14/kakaopay/success/" + order_id);
        params.add("fail_url", "http://3.36.36.14/kakaopay/fail/" + order_id);
        params.add("cancel_url", "http://3.36.36.14/kakaopay/cancel/" + order_id);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        PaymentResponseDto responseDto = restTemplate.postForObject("https://kapi.kakao.com/v1/payment/ready", requestEntity, PaymentResponseDto.class);
        if (responseDto == null) {
            throw new NullPointerException();
        }
        responseDto.setOrderId(order.getId());
        order.setOrderTid(responseDto.getTid());
        paymentPendingRepository.save(new PaymentPending(responseDto.getTid(), cid, order, member, total_amount));
        return responseDto;
    }

    public void cancel(Long orderId) {
        PaymentPending paymentPending = paymentPendingRepository.findByOrderId(orderId);
        paymentPending.getOrder().setOrderStatus(OrderStatus.CANCEL);
        paymentPendingRepository.delete(paymentPending);
    }

    public void cancelOrder(Long orderId) {
        PaymentPending paymentPending = paymentPendingRepository.findByOrderId(orderId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("tid", paymentPending.getTid());
        params.add("cancel_amount", paymentPending.getTotalPrice() + "");
        params.add("cancel_tax_free_amount", "0");
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject("https://kapi.kakao.com/v1/payment/cancel", requestEntity, KakaoPaymentCancelResponseDTO.class);
    }

    public void fail(Long orderId) {
        PaymentPending paymentPending = paymentPendingRepository.findByOrderId(orderId);
        paymentPendingRepository.delete(paymentPending);
    }

    public void sendEmail(Order order) {
        long totalPrice = 0;
        StringBuilder purchase = new StringBuilder();
        purchase.append("<h3>");
        for (OrderItem orderItem : order.getOrderItems()) {
            purchase.append("상품명 : ");
            purchase.append(orderItem.getItem().getItemNm());
            purchase.append("\t개수 : ");
            purchase.append(orderItem.getCount());
            purchase.append("<br>");
            totalPrice += orderItem.getTotalPrice();
        }
        purchase.append("</h3><h3>총 구매 가격 : ");
        purchase.append(totalPrice);
        purchase.append("</h3>");
        emailService.sendOrderEmail(order.getCreatedBy(), purchase.toString());
    }
}

