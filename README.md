# AICA Backoffice MVP

세션 로그인, 관리자 계정 발급, 3개 초기 역할, 기본 게시물 CRUD가 연결된 백오피스입니다. 최종 IA·시안·운영 정책은 미확정이며 범용 CMS를 구현하지 않았습니다.

Java 17 / Spring Boot 3.4.5 / Spring Framework 6.2.6 / Spring Security 6.4.5 / Tomcat 10.1.40 / MyBatis / Thymeleaf를 사용합니다. RTE 4.3.0은 Stage 0에서 검증한 서비스·데이터 접근 모듈만 사용합니다.

## 실행

프로젝트 루트에서 실행합니다. 동료 환경에서는 JDK 17과 `JAVA_HOME`이 필요합니다. Maven은 Wrapper가 준비합니다.

```powershell
.\scripts\mvn-local.ps1 -B -ntp -Pegov43-probe clean verify
.\scripts\run-dev.ps1 -Bootstrap
```

초기 관리자 이메일과 비밀번호를 입력한 뒤 <http://127.0.0.1:8080/login>에 접속합니다. 첫 로그인 후 비밀번호를 변경하고 다시 로그인합니다. 이미 계정이 있는 DB에서는 초기화가 계정을 덮어쓰지 않습니다.

다음 실행부터는 `.\scripts\run-dev.ps1`만 사용합니다. 종료는 `Ctrl+C`입니다. 개발 DB는 `.local-data/`의 H2 파일이며 재시작 후 데이터가 유지됩니다. 운영 DB 선택을 확정한 것은 아닙니다.

## 초기 권한

| 역할 | 계정 관리 | 게시물 관리 |
| --- | --- | --- |
| SUPER_ADMIN | 생성·목록·비활성화·초기화·별도 역할 변경 | 전체 |
| ADMIN | 불가 | 전체 |
| SUPPORTER | 불가 | 본인 |

신규 계정은 ADMIN 또는 SUPPORTER만 선택합니다. SUPER_ADMIN 승격은 별도 역할 변경 절차를 사용합니다. 세부 권한은 고객 확정사항이 아닌 초기안입니다.

## 문서

- [실행 방법·테스트 결과·변경 파일·미확정 사항](docs/MVP_DELIVERY.md)
- [처음 읽는 개발자를 위한 폴더·로그인·권한·저장 흐름 설명](docs/MVP_GUIDE.md)
- [개발 범위와 정책 제안](read.md)
- [보존된 Stage 0 검증 기록](docs/STAGE0_VERIFICATION.md)
- [동료의 원본 설계](docs/reference/README.md), [최초 비교 문서](docs/reference/README_COMPARISON.md)

전체 검증 명령은 Stage 0 7건과 MVP 7건, 총 14건을 실행합니다. Stage 0 기동 확인 화면은 `stage0` Spring 프로필에서만 사용합니다.

`.tools`, `.cache`, `target`, `.local-data`, 실제 비밀값이 담긴 `.env`는 저장소에 포함하지 않습니다. 감사 로그·로그인 실패 잠금·콘텐츠 추가 필드·기존 시스템 통합·운영 배포는 후속 범위입니다.
