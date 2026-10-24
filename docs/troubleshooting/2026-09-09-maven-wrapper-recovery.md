# Maven Wrapper 복구와 최초 baseline 검증

- 작업일: 2026-09-09
- 범위: 기존 애플리케이션 코드와 프레임워크 버전을 변경하지 않고 빌드 도구를 복구한다.

## 증상

저장소에는 `mvnw`와 `mvnw.cmd`가 있었지만 다음 명령이 Maven을 시작하지 못했다.

```shell
./mvnw test
```

확인된 오류는 다음과 같았다.

```text
.mvn/wrapper/maven-wrapper.properties: No such file or directory
ClassNotFoundException: org.apache.maven.wrapper.MavenWrapperMain
```

맥에는 Java 21이 설치되어 있었지만 별도의 `mvn` 명령은 설치되어 있지 않았다.

## 원인

Maven Wrapper는 프로젝트가 지정한 Maven 배포판을 내려받아 동일한 버전으로 빌드하게 해 주는 실행 도구다. 저장소에는 Wrapper 실행 스크립트만 있고, 다운로드 위치를 정의하는 `.mvn/wrapper/maven-wrapper.properties`가 없었다.

또한 Unix 계열 환경에서 `./mvnw`로 실행하는 데 필요한 실행 권한이 설정되어 있지 않았다.

## 최소 수정

- Maven Wrapper 3.2.0의 `only-script` 방식을 사용했다.
- Spring Boot 3.2.0 출시 시기와 맞는 Maven 3.9.5를 고정했다.
- `mvnw`에 실행 권한을 부여했다.
- 실행 중 자동 다운로드되는 `maven-wrapper.jar`는 Git에서 제외했다.

애플리케이션 소스, Java 버전, Spring Boot 버전과 의존성은 변경하지 않았다.

## 검증 결과

### Maven과 Java

```text
Apache Maven 3.9.5
Java 21.0.12.1 (Eclipse Temurin, ARM64)
```

### Clean build

```shell
./mvnw clean test
```

70개 Java 소스가 Java 21로 컴파일됐고 빌드는 성공했다. Annotation processor도 실행되어 QueryDSL 소스가 생성됐다.

테스트 실패는 없었지만 `src/test`가 존재하지 않아 실행된 테스트도 없었다. 따라서 이 결과는 컴파일 성공을 의미하며 기능 정상 동작을 증명하지 않는다.

### Package

```shell
./mvnw package
```

Spring Boot 실행 JAR `target/shop-0.0.1-SNAPSHOT.jar`가 생성됐다.

### 임시 기동 진단

기존 AWS RDS에 접근하지 않도록 다음 조건으로만 기동을 진단했다.

- H2 인메모리 DB
- 로컬 포트 18080
- `/tmp` 아래의 임시 이미지 경로
- 실제 인증정보가 아닌 OAuth 기동 검사용 문자열

결과는 다음과 같았다.

- Spring ApplicationContext 초기화 성공
- JPA EntityManagerFactory와 H2 스키마 생성 성공
- Embedded Tomcat 기동 성공
- `GET /` 응답: HTTP 200, `Shop` HTML 확인
- `GET /actuator/health` 응답: HTTP 401

`/actuator/health`의 401 응답은 현재 Spring Security가 health endpoint에도 인증을 요구한다는 뜻이다. 모니터링을 추가할 때 공개 범위와 접근 방식을 별도로 결정해야 한다.

## 당시 확인되지 않은 범위

- MariaDB에서의 실제 실행
- 기존 데이터와의 호환성
- 일반 로그인과 회원가입
- OAuth 로그인
- 이메일 발송
- KakaoPay 결제
- OpenAI API
- 상품 수집용 Chrome/Selenium

H2는 원인 분리를 위한 임시 진단 도구로만 사용했다. 이후 baseline 환경은 기존 데이터베이스 계열을 유지하는 로컬 MariaDB를 사용했다.

## 후속 작업 결과

이후 `local` profile과 MariaDB 10.11을 구성해 애플리케이션 기동을 확인했다. 기능별 검증 현황과 다음 작업은 `docs/roadmap.md`에 기록한다.
