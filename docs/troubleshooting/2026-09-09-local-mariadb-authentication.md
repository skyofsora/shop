# 로컬 MariaDB 인증 오류 해결

- 작업일: 2026-09-09
- 환경: Spring Boot `local` profile, MariaDB 10.11

## 증상

애플리케이션 기동 중 JPA가 데이터베이스 연결을 만들지 못했다.

```text
Access denied for user 'shop_app'@'localhost' (using password: YES)
```

Maven은 마지막에 `BUILD FAILURE`를 출력했지만, 실제 원인은 Maven 자체가 아니라 MariaDB 인증 실패였다.

## 원인 분리

- MariaDB 서비스는 실행 중이었다.
- `shop_app@localhost` 계정과 `shop_local` 데이터베이스가 존재했다.
- 해당 계정으로 `shop_local`에 직접 접속한 이력이 있어 계정과 권한 설정은 동작했다.
- `using password: YES`는 애플리케이션이 비밀번호를 전달했지만 서버가 거절했다는 뜻이다.

MariaDB 계정의 비밀번호를 변경한 뒤, 터미널의 `SHOP_DB_PASSWORD` 환경변수에는 이전 값이 남아 있었다.

## 해결

환경변수를 지운 뒤 현재 비밀번호를 다시 입력했다. 실제 비밀번호는 명령 기록과 저장소에 남기지 않았다.

```shell
unset SHOP_DB_PASSWORD
read -s "SHOP_DB_PASSWORD?현재 shop_app 비밀번호: "
export SHOP_DB_PASSWORD
echo
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

이후 Spring Boot 애플리케이션 기동을 확인했다.

## 재발 시 확인 순서

1. `brew services list`에서 MariaDB 실행 상태를 확인한다.
2. 오류의 첫 번째 `Caused by`와 SQL 오류 번호를 확인한다.
3. 비밀번호 변경 후라면 현재 터미널의 환경변수를 다시 입력한다.
4. 비밀번호를 소스 코드, 설정 파일, 채팅 또는 명령행 인수에 직접 넣지 않는다.
