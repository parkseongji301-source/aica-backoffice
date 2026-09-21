# AICA Backoffice

백오피스의 **0단계 기술 검증과 최소 프로젝트**입니다. 인증·계정·게시물 기능은 아직 구현하지 않았습니다.

Java 17 / Spring Boot 3.4.5 / Spring Framework 6.2.6 / Spring Security 6.4.5 / Tomcat 10.1.40을 사용합니다. RTE 4.3.0은 별도 Maven 프로필에서 일부 서비스·데이터 접근 기능을 검증했습니다. RTE 전체 호환성과 운영 DB 구성은 미확정입니다.

## 시작하기

JDK 17을 준비하고 `JAVA_HOME`을 설정합니다. Maven은 Wrapper가 내려받으므로 따로 설치하지 않아도 됩니다.

```powershell
git clone https://github.com/parkseongji301-source/aica-backoffice.git
Set-Location aica-backoffice

# 기본 검증 4건과 실행 JAR 생성
.\mvnw.cmd -B -ntp clean verify

# RTE 포함 검증 총 7건과 실행 JAR 생성
.\mvnw.cmd -B -ntp -Pegov43-probe clean verify

& "$env:JAVA_HOME\bin\java.exe" -jar .\target\backoffice-stage0-0.0.1-SNAPSHOT.jar
```

기동 확인: <http://127.0.0.1:8080/__stage0/status>

다른 경로는 검증용 보안 설정에 따라 차단됩니다. 로그인 계정은 없습니다. RTE·MyBatis·Flyway·H2는 테스트 범위에만 포함되며 실행 JAR에는 포함되지 않습니다. 최초 빌드에는 외부 Maven 저장소 연결이 필요합니다.

## 문서

- [현재 개발 제안과 전체 1차 범위](read.md)
- [0단계 검증 결과·의존성 조정·자세한 실행 방법](docs/STAGE0_VERIFICATION.md)
- [동료가 전달한 원본 설계 문서](docs/reference/README.md)
- [최초 제안과 원본의 비교](docs/reference/README_COMPARISON.md)

원본 설계와 비교 문서는 참고 자료이며, 현재 구현 상태는 이 README와 0단계 검증 문서를 기준으로 확인합니다. 역할과 세부 운영 정책은 초기 제안입니다.

`.tools`, `.cache`, `target`과 실제 비밀값이 담긴 `.env`는 저장소에서 제외합니다. 검증용 로컬 JDK는 공유하지 않으므로 각 개발자가 JDK 17을 준비해야 합니다.
