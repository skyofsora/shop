# Shop 프로젝트

Java와 Spring Boot로 만든 웹 쇼핑몰 프로젝트입니다. 현재는 기존 코드를 로컬 개발 환경에서 실행하고 동작을 확인하는 단계입니다.

## 개발 환경

- Java 21
- MariaDB 10.11
- Maven Wrapper (`./mvnw`)
- macOS 기준 로컬 프로필: `local`

## 로컬 실행

### 1. MariaDB 시작

```bash
brew services start mariadb@10.11
```

로컬 MariaDB에 `shop_local` 데이터베이스와 `shop_app` 계정이 준비되어 있어야 합니다.

### 2. DB 비밀번호 설정

프로젝트 루트의 zsh 터미널에서 입력합니다. 입력 중에는 글자가 화면에 표시되지 않습니다.

```bash
unset SHOP_DB_PASSWORD
read -s "SHOP_DB_PASSWORD?MariaDB password: "
export SHOP_DB_PASSWORD
echo
```

실제 비밀번호는 저장소 파일에 적지 않습니다.

### 3. Gmail 앱 비밀번호 설정

메일 인증을 테스트할 때 Google 계정의 일반 비밀번호가 아닌 앱 비밀번호를 사용합니다. 앱 비밀번호는 저장소 파일에 적지 않습니다.

```bash
unset SHOP_MAIL_PASSWORD
read -s "SHOP_MAIL_PASSWORD?Gmail app password: "
export SHOP_MAIL_PASSWORD
echo
```

### 4. Spring Boot 실행

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

로그에 `Started ShopApplication`이 나오면 실행된 것입니다. 서버를 끄려면 터미널에서 `Ctrl+C`를 누릅니다.

## 로컬 화면

- 메인: <http://localhost:8080/>
- 로그인 화면: <http://localhost:8080/members/login>
- 회원가입 화면: <http://localhost:8080/members/new>

업로드 파일은 프로젝트의 `.local/uploads/` 아래에 저장되며 Git에는 포함되지 않습니다. 현재 local 프로필에서는 OAuth 로그인을 사용할 수 없습니다.

## 확인된 제한사항

- 테스트 코드가 없어 `./mvnw test` 성공만으로 기능 정상 여부를 보장하지 않습니다.
- 일반 가입과 OAuth 신규 가입의 기본 역할을 `USER`로 설정했고, Java 21 컴파일을 확인했습니다. 일반 회원가입과 로그인을 로컬에서 확인했습니다.
- 테스트 계정의 DB 역할값은 별도로 조회하지 않았습니다.
- 로컬에서 Gmail 메일 인증과 일반 회원가입을 확인했습니다. KakaoPay 결제, OAuth, OpenAI 연동은 아직 검증하지 않았습니다.
- 공개 배포 전 주문 소유권 검사 등 보안 점검과 기능 검증이 더 필요합니다.

## 로컬 검증 기록 (2026-10-20)

- Gmail 메일 인증 코드 수신 및 인증 완료
- 일반 회원가입 완료
- 가입한 계정으로 로그인 완료
