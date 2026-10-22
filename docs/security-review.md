# 보안 점검 메모

- 마지막 갱신: 2026-10-22
- 범위: 신규 회원 역할, 주문 조회·취소, KakaoPay 결제 준비·callback·환불 경로
- 방법: 코드 읽기만 수행. 실제 결제와 회원 데이터 변경은 하지 않음.

## 조치 완료: 신규 회원 기본 역할

`Member` 생성자와 `Member.createMember()`가 모두 `Role.ADMIN`을 지정하고 있었다. 일반 가입은 `MemberController`에서 `createMember()`를 호출하고, OAuth 신규 가입은 `OAuthAttributes`에서 생성자를 사용한다.

두 경로의 기본 역할을 `Role.USER`로 변경했고, `./mvnw -DskipTests clean compile`로 70개 소스의 컴파일을 확인했다. 2026-10-20에 일반 회원가입과 로그인을 확인했으며, 테스트 계정의 DB 역할값은 별도로 조회하지 않았다.

## 조치 완료: 결제 준비 시 주문 소유권 확인 (코드 추가 2026-10-16, 누락 처리 확인 2026-10-22)

주문 내역은 `OrderRepository.findOrders(email, ...)`로 로그인한 이메일에 한정하고, 일반 주문 취소는 `OrderService.validateOrder(orderId, email)`를 호출해 소유자를 확인한다.

`GET /kakaopay/purchase/{order_id}`에서 호출하는 `KakaoPaymentService.purchase(orderId, email)`에 검사를 추가했다. 결제 대기 정보가 없으면 HTTP 404로 종료한다. 로그인 이메일이 결제 대기 정보의 회원 이메일과 주문 소유자의 이메일 양쪽과 일치해야 하며, 일치하지 않으면 HTTP 403으로 거절한다. 결제 대기 정보는 소유권 검사 뒤에 삭제한다.

2026-10-22에 `./mvnw -DskipTests clean compile`로 Java 21에서 70개 소스 컴파일을 확인했다. 실제 KakaoPay 결제 API와 타 사용자 계정 요청은 실행하지 않았다. 이 검사는 결제 준비 조회 경로에 한정되며 아래의 callback·환불 문제는 해결하지 않는다.

## 추가 조치 필요: 결제 callback과 환불 경로

- 실제 컨트롤러 경로는 `/kakaopay/**`다. `SecurityConfig`의 `/kakao/**` 역할·CSRF 예외와 일치하지 않으므로 이 경로들은 기본 규칙인 로그인 필요가 적용되고 CSRF 예외도 적용되지 않는다. 따라서 공개 경로라고 단정할 수는 없지만, 로그인 상태만으로 주문 소유권이 확인되지는 않는다.
- `/kakaopay/fail/{order_id}`와 `/cancel/{order_id}`는 `Principal`이나 주문 소유자를 확인하지 않고 `orderId`로 결제 대기 정보를 삭제하거나 주문 상태를 변경한다. 다른 로그인 사용자가 다른 주문 ID로 호출하지 못하도록 경로 수준의 소유권 또는 callback 검증이 필요하다.
- `/kakaopay/success/{order_id}`도 메서드에서 `Principal`을 확인하지 않는다. 서비스는 외부 승인 API를 호출하기 전에 주문 상태를 `ORDER`로 설정하고, API 응답 본문의 승인 성공 여부는 확인하지 않는다. 공식 단건 결제 흐름에서 `pg_token`은 브라우저가 승인 URL로 돌아올 때 전달되며, 가맹점은 저장해 둔 `tid`와 `pg_token`을 승인 API에 보내야 한다. 승인 요청의 `partner_order_id`와 `partner_user_id`도 준비 단계에서 사용한 값과 일치해야 한다. 따라서 브라우저의 성공 경로 진입만으로 주문을 완료 처리하면 안 되며, 승인 응답과 저장된 결제·주문 정보를 대조한 뒤 주문 상태를 확정해야 한다 ([카카오페이 단건 결제 문서](https://developers.kakaopay.com/docs/payment/online/single-payment)).
- 준비 요청은 `order.getCreatedBy()`를 `partner_user_id`로 보내고 승인 요청은 `paymentPending.getMember().getEmail()`을 보낸다. 현재 일반 주문 흐름에서는 인증 사용자명이 회원 이메일이므로 두 값은 일치할 것으로 보이지만, 코드가 그 일치를 명시적으로 검사하지는 않는다. 승인 응답의 주문·사용자 식별자와 결제 금액을 저장된 주문 정보와 대조해야 한다.
- `/kakaopay/refund/{order_id}`는 `KakaoPayController`에서 실제 환불 처리를 하지 않으면서 성공 문구를 반환한다. 실제 환불 API를 구현하기 전에는 성공처럼 응답하지 않도록 바꿔야 한다. 결제 취소 API와 소유권 검증은 아직 구현하지 않았다.

현재 서비스 코드는 `kapi.kakao.com/v1/payment/approve`와 Admin Key 형식의 구형 연동을 사용하지만, 현재 공식 온라인 단건 결제 문서는 `/online/v1/payment/approve`와 Secret Key 형식을 안내한다. 문서상 승인 요청·응답 기준은 참고하되, 인증 방식과 주소를 현재 코드에 그대로 옮기지는 않는다. 기존 가맹점 연동 버전에 맞는 문서를 확인한 뒤 API 교체 여부를 별도 결정해야 한다.

공식 취소 문서는 결제 취소 API에 `tid`와 취소 금액이 필요하며 결과 상태를 확인하도록 안내한다 ([카카오페이 결제 취소 문서](https://developers.kakaopay.com/docs/payment/online/cancellation)). 현재 환불 경로에는 이 호출이 없으므로, 성공 문구를 반환하는 동작을 먼저 고쳐야 한다.

실제 결제 요청은 실행하지 않았다. callback에 로그인 세션이 필요한지와 소유권을 어떤 값으로 확인할지는 현재 가맹점 연동 방식 및 브라우저 반환 흐름까지 확인한 후 설계한다.
