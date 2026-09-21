# 백오피스 MVP 전달·검증 문서

작성일: 2026-09-21  
범위: 명시한 초기 정책으로 연결한 로그인·계정 관리·3역할·기본 게시물 CRUD  
상태: 개발 환경 구현·검증 완료. 최종 IA·운영 정책 승인이나 운영 배포 완료는 아님.

## 1. 구현한 동작

- 세션 로그인·POST 로그아웃, BCrypt 저장, 본인 비밀번호 변경과 재로그인.
- 명시적 최초 SUPER_ADMIN 초기화. 기존 계정이 있으면 재실행해도 덮어쓰지 않음.
- SUPER_ADMIN의 계정 목록·ADMIN/SUPPORTER 발급·비활성화·임시 비밀번호 초기화·별도 역할 변경.
- 임시 비밀번호 한 번 표시와 변경 전 업무 요청 차단.
- SUPER_ADMIN과 ADMIN은 전체 게시물, SUPPORTER는 본인 게시물만 목록·상세·수정·삭제. 세 역할 모두 등록 가능.
- 제목·일반 텍스트 본문·작성자·시각·삭제 시각을 이용한 게시물 CRUD, 10건 단위 목록.
- CSRF 보호·텍스트 출력·입력 검증, 계정 변경 후 기존 세션 무효화, 자기 변경/마지막 관리자 보호.
- 파일 DB에 저장한 업무 데이터가 서버 재시작 후 유지됨.

이번 MVP에 감사 로그 테이블, 실패 횟수 잠금, 계정 이름 수정·재활성화, 복잡한 권한 테이블, 공개 가입, SSO, 콘텐츠 추가 필드, CI·운영 배포는 포함하지 않았다. 3개 역할과 ADMIN의 전체 게시물 관리는 고객 확정사항이 아닌 현재 구현 초기안이다.

## 2. 실행하기 — Windows PowerShell

프로젝트 루트에서 실행한다. 이 PC의 실제 경로는 다음과 같다.

```powershell
Set-Location 'C:\Users\sfsf1\OneDrive\Desktop\exex1\BOFC\AICA-back-office-main'

# Stage 0을 포함한 전체 검증과 실행 JAR 생성
.\scripts\mvn-local.ps1 -B -ntp -Pegov43-probe clean verify

# 빈 개발 DB에 최초 관리자 생성 후 서버 실행
.\scripts\run-dev.ps1 -Bootstrap
```

마지막 명령은 관리자 이메일과 초기 비밀번호를 직접 입력받는다. 비밀번호는 화면에 표시하지 않으며 파일에 기록하지 않는다. 12자 이상, UTF-8 기준 72바이트 이하를 사용한다. 이미 계정이 있는 DB에서는 새 초기 관리자를 만들거나 기존 비밀번호를 바꾸지 않는다.

브라우저에서 <http://127.0.0.1:8080/login>을 연다. 입력한 계정으로 로그인하고 초기 비밀번호를 바꾼 뒤 다시 로그인한다. 계정 관리 메뉴에서 ADMIN과 SUPPORTER를 발급하면 각각 임시 비밀번호가 한 번 표시된다.

서버 종료는 실행한 터미널에서 `Ctrl+C`다. 다음부터는 초기화 없이 실행한다.

```powershell
.\scripts\run-dev.ps1
```

현재 PC에서는 `.tools/jdk/`의 검증용 JDK를 찾도록 보조 스크립트를 제공한다. 동료는 JDK 17을 설치하고 자신의 `JAVA_HOME`을 설정해야 한다. 이 경로와 DB 파일은 Git에 포함되지 않는다. 실행 정책으로 스크립트가 차단되는 환경에서는 조직 정책을 따르며, JDK 17과 `JAVA_HOME` 설정 후 표준 Wrapper·Java 명령을 사용할 수 있다.

```powershell
.\mvnw.cmd -B -ntp -Pegov43-probe clean verify
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\backoffice-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

직접 Java로 최초 초기화를 할 때는 현재 프로세스 환경에 아래 변수를 제공해야 한다. 실제 값은 소스·커밋·명령 예시에 적지 않는다. 보조 스크립트는 이 설정과 정리를 대신한다.

| 환경 변수 | 의미 |
| --- | --- |
| BACKOFFICE_BOOTSTRAP_ENABLED | 빈 DB에 초기화할 때만 true |
| BACKOFFICE_BOOTSTRAP_EMAIL | 최초 관리자 이메일 |
| BACKOFFICE_BOOTSTRAP_PASSWORD | 최초 임시 비밀번호 |
| BACKOFFICE_BOOTSTRAP_NAME | 표시 이름, 기본값 최상위 관리자 |
| BACKOFFICE_DEV_DB_URL | 개발 H2 파일 위치를 바꿀 때 사용 |
| BACKOFFICE_DEV_DB_PASSWORD | 개발 DB 비밀번호, 기본 로컬 설정은 빈 값 |

## 3. 개발 DB와 운영 DB 구분

기본 개발 DB는 `jdbc:h2:file:./.local-data/backoffice`이며 실제 파일은 `.local-data/backoffice.mv.db`다. 실행 스크립트는 항상 프로젝트 루트에서 실행해 DB 경로가 달라지는 일을 막는다. 개발 서버는 기본적으로 `127.0.0.1:8080`에만 바인딩하고 H2 웹 콘솔은 제공하지 않는다.

- 이 DB 파일을 지우면 해당 개발 계정·게시물도 사라진다.
- 서버를 재시작하면 로그인 세션은 사라지지만 DB 데이터는 남는다.
- 테스트는 별도의 인메모리 DB·임시 파일 DB를 사용하며 개발 데이터에 연결하지 않는다.
- 현재 마이그레이션은 `db/migration/h2/`의 개발용 SQL이다.
- 운영 DB 종류·드라이버·접속 설정·SQL·백업·배포·다중 서버 세션은 미확정이다. 운영 DB를 H2로 확정한 것이 아니다.
- PostgreSQL 16은 앞선 제안에 있었으나 이번 구현에서 연결·호환성을 검증하지 않았다.

## 4. Stage 0 보존과 실제 RTE 적용

[Stage 0 검증 문서](STAGE0_VERIFICATION.md)의 원래 본문과 검증 코드를 보존했다. Stage 0 당시의 전체 프로젝트는 Git 커밋 `6ed6fd6`에도 남아 있다.

현재는 `psl.dataaccess`와 `fdl.cmmn` RTE 4.3.0을 실행 의존성으로 사용한다. 매퍼는 `EgovAbstractMapper`를 상속하고, 서비스는 `EgovAbstractServiceImpl`의 메시지 예외 처리를 사용한다. MyBatis-Spring 3.0.4와 기존의 전이 의존성 제외 조건을 유지했다. RTE MVC/JSP나 다른 RTE 모듈로 사용 범위를 늘리지 않았다.

업무 코드는 `mvp/`, 원래 기동 확인 코드는 `stage0/`다. 현재 JAR를 Stage 0 방식으로 실행하려면 다음처럼 지정한다. 일반 업무 서버와 동시에 같은 포트를 사용할 수 없으므로 먼저 종료한다.

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\backoffice-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage0
```

이때만 `/__stage0/status`가 공개되고 업무 기능은 등록되지 않는다. `egov43-probe`는 RTE 추가 테스트를 포함하는 **Maven 프로필**, `stage0`와 `dev`는 실행 구성을 선택하는 **Spring 프로필**이다.

## 5. 직접 실행한 검증

실행 명령: `.\scripts\mvn-local.ps1 -o -B -ntp -Pegov43-probe clean verify`

결과: **14 tests / 0 failures / 0 errors / 0 skipped / BUILD SUCCESS**. `-o`는 필요한 의존성을 먼저 내려받은 이 PC에서 사용한 오프라인 옵션이다. 처음 받는 환경에서는 제거한다.

| 테스트 | 개수 | 확인 내용 |
| --- | ---: | --- |
| Stage0WebTest | 3 | 요청한 버전·실제 Tomcat 렌더링·원래 접근 차단 |
| Stage0DatabaseTest | 1 | 원래 Flyway·MyBatis·Spring 트랜잭션 커밋/롤백 |
| Egov43CompatibilityTest | 3 | RTE 매퍼·서비스·Resource 주입과 롤백 |
| BackofficeIntegrationTest | 6 | 실제 HTTP 로그인/CSRF/쿠키 세션, 계정 발급·권한·게시물 CRUD·암호 변경·비활성화·초기화·승격·동시 강등 보호 |
| PersistenceRestartTest | 1 | 파일 DB로 서버 전체 종료/재기동, 계정 3개·비밀번호 해시·게시물 유지와 재로그인 |

아래 완료 조건은 모의 로그인 대신 실제 로그인 폼과 세션 쿠키로 확인했다.

| 요청한 완료 흐름 | 결과 |
| --- | --- |
| SUPER_ADMIN 로그인, ADMIN/SUPPORTER 생성과 각각 로그인 | 통과 |
| ADMIN/SUPPORTER의 계정 관리 접근 차단 | GET·POST 모두 통과 |
| ADMIN 게시물 등록·수정·삭제 | 통과, 타인 게시물 수정·삭제도 초기안대로 허용 |
| SUPPORTER 등록·본인 글 수정·삭제 | 통과 |
| SUPPORTER의 타인 글 상세·수정·삭제 차단 | 통과, 목록·개수·페이지에서도 제외 |
| 본인 비밀번호 변경·기존 비밀번호 거부·로그아웃 | 통과 |
| 서버 종료 후 같은 파일 DB로 다시 시작 | 계정·게시물 유지 통과 |

추가로 패키징한 JAR를 격리된 테스트 DB로 실행하고 Browser 스킬로 로그인·계정 생성/목록·게시물 등록/상세/수정·로그아웃 화면을 확인했다. JAR 프로세스를 종료한 뒤 `run-dev.ps1`로 같은 DB를 다시 열었고, 재로그인 후 발급한 계정과 수정한 게시물이 유지되는 것도 확인했다. 최종 시안 검수나 운영 브라우저 전체 조합 테스트를 의미하지 않는다.

테스트 보고서는 `target/surefire-reports/`, 이 PC의 실행 로그는 `.cache/mvp-verify.log`다. 이전에 관측된 Flyway/H2 지원 범위 경고는 남아 있지만 실제 마이그레이션과 DB 테스트는 통과했다. RTE 매퍼의 final 초기화 메서드에 대한 Spring 프록시 경고도 관측했으며 업무 쿼리와 트랜잭션 실행 결과를 별도로 검증했다. 공식적으로 모든 RTE 기능이 호환된다는 결론은 아니다.

## 6. 미확정 사항과 후속 범위

- 역할 수·명칭, ADMIN의 타인 글 관리와 SUPPORTER의 조회 범위.
- 실제 IA·콘텐츠 필드·화면 시안·공개/승인/보존/복구 정책.
- 운영 DB·기존 테이블·데이터 이전·공식 표준 프로젝트 및 추가 RTE 기능.
- 기존 시스템 로그인/SSO·사용자 모듈 통합과 배포 구성.
- 로그인 실패 제한·잠금, 계정 재활성화, 감사 기록, HTTPS/Secure 쿠키, 다중 서버 세션.
- 동료 PC에서의 실행 확인, CI와 운영 배포.

`auth_version`과 단일 계정 변경 잠금 행은 계정 변경 후 접근 제한과 마지막 관리자 보호에 필요한 범위로만 사용했다. 범용 권한·감사·콘텐츠 시스템으로 확장하지 않았다.

## 7. 생성·수정 파일 목록

아래 목록은 이번 MVP 작업의 파일을 기준으로 작성한다. 원본 참고 README와 Stage 0 DB/RTE 검증 파일의 내용은 유지했다.

수정 10개, 생성 48개입니다. 아래 경로는 프로젝트 루트 기준입니다.

```text
수정  .gitignore
수정  README.md
수정  docs/STAGE0_VERIFICATION.md
수정  pom.xml
수정  read.md
수정  src/main/java/egovframework/backoffice/BackofficeApplication.java
수정  src/main/java/egovframework/backoffice/stage0/Stage0Controller.java
수정  src/main/java/egovframework/backoffice/stage0/Stage0SecurityConfiguration.java
수정  src/main/resources/application.yml
수정  src/test/java/egovframework/backoffice/verification/Stage0WebTest.java
생성  docs/MVP_DELIVERY.md
생성  docs/MVP_GUIDE.md
생성  scripts/run-dev.ps1
생성  src/main/java/egovframework/backoffice/mvp/account/Account.java
생성  src/main/java/egovframework/backoffice/mvp/account/AccountController.java
생성  src/main/java/egovframework/backoffice/mvp/account/AccountMapper.java
생성  src/main/java/egovframework/backoffice/mvp/account/AccountService.java
생성  src/main/java/egovframework/backoffice/mvp/account/InitialAdminInitializer.java
생성  src/main/java/egovframework/backoffice/mvp/account/IssuedCredential.java
생성  src/main/java/egovframework/backoffice/mvp/account/Role.java
생성  src/main/java/egovframework/backoffice/mvp/auth/AuthController.java
생성  src/main/java/egovframework/backoffice/mvp/common/BusinessException.java
생성  src/main/java/egovframework/backoffice/mvp/common/InputRules.java
생성  src/main/java/egovframework/backoffice/mvp/common/WebAdvice.java
생성  src/main/java/egovframework/backoffice/mvp/config/MvpConfiguration.java
생성  src/main/java/egovframework/backoffice/mvp/post/Post.java
생성  src/main/java/egovframework/backoffice/mvp/post/PostController.java
생성  src/main/java/egovframework/backoffice/mvp/post/PostMapper.java
생성  src/main/java/egovframework/backoffice/mvp/post/PostPage.java
생성  src/main/java/egovframework/backoffice/mvp/post/PostService.java
생성  src/main/java/egovframework/backoffice/mvp/security/AccessPolicy.java
생성  src/main/java/egovframework/backoffice/mvp/security/AccountDetailsService.java
생성  src/main/java/egovframework/backoffice/mvp/security/AccountPrincipal.java
생성  src/main/java/egovframework/backoffice/mvp/security/AccountSessionFilter.java
생성  src/main/java/egovframework/backoffice/mvp/security/CurrentAccount.java
생성  src/main/java/egovframework/backoffice/mvp/security/SecurityConfiguration.java
생성  src/main/resources/application-dev.yml
생성  src/main/resources/application-stage0.yml
생성  src/main/resources/db/migration/h2/V1__backoffice.sql
생성  src/main/resources/mapper/AccountMapper.xml
생성  src/main/resources/mapper/PostMapper.xml
생성  src/main/resources/messages.properties
생성  src/main/resources/static/css/app.css
생성  src/main/resources/templates/accounts/form.html
생성  src/main/resources/templates/accounts/issued.html
생성  src/main/resources/templates/accounts/list.html
생성  src/main/resources/templates/accounts/role.html
생성  src/main/resources/templates/auth/login.html
생성  src/main/resources/templates/auth/password.html
생성  src/main/resources/templates/error.html
생성  src/main/resources/templates/fragments.html
생성  src/main/resources/templates/posts/detail.html
생성  src/main/resources/templates/posts/form.html
생성  src/main/resources/templates/posts/list.html
생성  src/test/java/egovframework/backoffice/integration/BackofficeIntegrationTest.java
생성  src/test/java/egovframework/backoffice/integration/HttpBrowser.java
생성  src/test/java/egovframework/backoffice/integration/PersistenceRestartTest.java
생성  src/test/resources/application-test.yml
```

코드를 처음 읽는 순서와 각 파일의 연결은 [MVP 코드 읽기 가이드](MVP_GUIDE.md)에 정리했다.
