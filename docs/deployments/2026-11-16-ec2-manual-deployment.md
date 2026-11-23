# EC2 수동 배포 기록

- 작업일: 2026-11-16
- 리전: Asia Pacific (Tokyo), `ap-northeast-1`
- 목적: 로컬에서 검증한 Spring Boot 쇼핑몰을 단일 Linux 서버에 수동 배포하고, 접속·보안·운영 상태를 증명한다.

## 문제와 선택

기존 애플리케이션은 로컬 MariaDB에서만 실행됐고, 실행 프로세스와 외부 HTTP 요청이 연결되는 운영 환경이 없었다.

첫 배포에서는 비용과 복잡도를 낮추기 위해 애플리케이션과 MariaDB를 한 EC2 인스턴스에 함께 둔다. 이는 고가용성 구조는 아니지만, Linux 서비스 관리, reverse proxy, 보안 그룹, 로그와 상태 확인을 한 번에 검증할 수 있다.

## 구성

| 영역 | 선택 | 이유 |
| --- | --- | --- |
| OS | Amazon Linux 2023 x86_64 | AWS 환경과 Java 21 패키지 지원을 우선했다. |
| 컴퓨팅 | EC2 `t3.micro` | Free Plan 범위에서 첫 수동 배포를 검증한다. |
| 디스크 | 암호화된 8 GiB gp3 EBS | OS, JAR, MariaDB 데이터를 재시작 후에도 보존한다. |
| 원격 접속 | Systems Manager Session Manager | SSH 키와 인바운드 22번 포트 없이 관리한다. |
| 앱 실행 | systemd `shop.service` | 재부팅 후 자동 시작과 실패 시 재시작을 제공한다. |
| 프록시 | Nginx 80 → Spring Boot 8080 | 애플리케이션 포트 8080을 외부에 직접 공개하지 않는다. |
| 데이터베이스 | 인스턴스 내부 MariaDB | 첫 단계의 비용과 운영 복잡도를 낮춘다. |

## 보안 설정

- IAM 역할 `shop-ec2-ssm-role`에 `AmazonSSMManagedInstanceCore`를 연결했다.
- EC2는 IMDSv2를 필수로 설정했다.
- SSH 키 페어와 인바운드 TCP 22 규칙을 사용하지 않았다.
- 첫 외부 검증에서는 HTTP TCP 80을 현재 작업자 공인 IP `/32`에만 허용했다. 공인 IP가 바뀌면 보안 그룹의 소스도 갱신해야 한다.
- DB·Gmail·OAuth 관련 실제 값은 `/etc/shop/shop.env`에만 두고, 소유자 `root`, 그룹 `ec2-user`, 권한 `640`으로 설정했다. 저장소에는 변수명만 있는 `deploy/shop.env.example`만 둔다.

## 구현 과정

1. Java 21, MariaDB, Nginx, Git을 설치하고 MariaDB를 자동 시작하도록 설정했다.
2. `shop_local` 데이터베이스와 `shop_app@localhost` 전용 계정을 생성했다.
3. GitHub 저장소를 `/opt/shop/source`에 `ec2-user` 권한으로 clone하고, Maven Wrapper로 실행 JAR를 빌드했다.
4. JAR를 `/opt/shop/shop.jar`에 배치하고 `shop.service`를 `/etc/systemd/system/`에 등록했다.
5. 이미지 업로드 디렉터리 `/home/ec2-user/shop/uploads/item`과 비밀 설정 디렉터리 `/etc/shop`을 만들고 권한을 분리했다.
6. Nginx 설정을 `/etc/nginx/conf.d/shop.conf`에 배치했다. Amazon Linux 기본 서버 설정과 겹치지 않도록 별도 설정의 `server_name _`을 제거했다.

## 검증 결과

| 검증 | 결과 |
| --- | --- |
| MariaDB service | `enabled`, `active` |
| 앱 DB 계정 | `shop_app@localhost`로 `shop_local` 접속 성공 |
| systemd 앱 서비스 | `shop.service`가 `enabled`, `active (running)` |
| Spring Boot | Tomcat 8080 기동 확인 |
| 내부 health | `http://127.0.0.1:8080/actuator/health` → HTTP 200 / `UP` |
| Nginx proxy | `http://127.0.0.1/actuator/health` → HTTP 200 / `UP` |
| 외부 health | 작업자 IP에서 Nginx 80 경유 HTTP 200 / `UP` |
| 중지 후 복구 | EC2 시작 후 MariaDB·shop·Nginx가 모두 `enabled` / `active`, 새 public IP에서 외부 health HTTP 200 / `UP` |
| 실제 사용자 흐름 | 외부 회원가입·메일 인증·로그인 후 관리자 권한으로 상품과 대표 이미지를 등록하고, 공개 상품 상세 화면에서 표시 확인 |

메일 health가 처음 `DOWN`이었고, 원인은 Google 앱 비밀번호의 공백 포함 입력이었다. 공백 없는 앱 비밀번호로 환경변수를 갱신한 뒤 DB, disk, mail health가 모두 `UP`인 것을 확인했다. 진단 중에만 health 세부 정보를 로컬에서 확인했고, 완료 후에는 다시 숨겼다.

2026-11-17에는 인스턴스를 중지한 뒤 다시 시작해 자동 복구를 검증했다. `mariadb`, `shop`, `nginx`가 모두 `enabled`와 `active` 상태였고, 새 public IPv4와 변경된 작업자 IP `/32` 보안 그룹 규칙에서 외부 health가 다시 HTTP 200 / `UP`을 반환했다.

2026-11-19에는 비어 있는 EC2 MariaDB가 새 배포 환경임을 먼저 확인했다. 이후 외부 브라우저에서 메일 인증 회원가입과 로그인을 완료하고, 해당 검증 계정에만 관리자 권한을 부여했다. 관리자 화면에서 상품과 대표 이미지를 등록한 뒤, 외부 상품 상세 페이지에서 이름·가격·이미지·수량 선택 화면이 정상 표시되는 것을 확인했다. 이 과정으로 reverse proxy와 health check뿐 아니라, DB 쓰기·파일 업로드·공개 정적 이미지 제공까지 실제 사용자 요청 흐름으로 검증했다.

## 남은 한계와 다음 작업

- 단일 EC2와 단일 MariaDB이므로 장애 격리·자동 복구·백업이 없다.
- HTTP 80은 작업자 IP에만 제한되어 있으며, 도메인·HTTPS·공개 접근 정책은 아직 구성하지 않았다.
- `spring.jpa.hibernate.ddl-auto=update`를 사용 중이다. 이후 Flyway 같은 migration 도구와 `validate` 전환을 검토한다.
- Docker, CI, Terraform, CloudWatch 로그·알람은 후속 단계다.
- 인스턴스를 중지하면 컴퓨팅 사용은 멈추지만 EBS는 유지된다. 종료·볼륨 삭제는 서버와 DB 데이터를 잃으므로 별도 정리 절차로 다룬다.
