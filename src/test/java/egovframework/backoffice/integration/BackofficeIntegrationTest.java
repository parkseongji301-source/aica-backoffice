package egovframework.backoffice.integration;

import egovframework.backoffice.mvp.account.*;
import egovframework.backoffice.mvp.post.*;
import egovframework.backoffice.mvp.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static egovframework.backoffice.integration.HttpBrowser.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BackofficeIntegrationTest {
    // Test fixtures only; not deployed/bootstrap credentials.
    private static final String ROOT = "root@example.test";
    private static final String INITIAL = "Test-Initial-Root42";
    private static final String PASSWORD = "Test-Changed-Root42";
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccountService accounts;
    @Autowired AccountMapper accountMapper;
    @Autowired PostService posts;
    @Autowired PasswordEncoder encoder;

    @BeforeEach void reset() {
        jdbc.update("DELETE FROM posts");
        jdbc.update("DELETE FROM users");
        accounts.bootstrap(ROOT, "최상위 테스트", INITIAL);
    }

    private HttpBrowser root() throws Exception {
        var root = new HttpBrowser(port);
        root.login(ROOT, INITIAL, "/account/password");
        root.password(INITIAL, PASSWORD);
        root.login(ROOT, PASSWORD, "/admin/posts");
        return root;
    }
    private AccountPrincipal principal(String email) { return new AccountPrincipal(accountMapper.findByEmail(email)); }
    private HttpBrowser member(HttpBrowser root, String email, Role role) throws Exception {
        redirect(root.post("/admin/accounts", Map.of("email", email, "displayName", email, "role", role.name())),
                "/admin/accounts/issued");
        String temporary = root.issued();
        redirect(root.get("/admin/accounts/issued"), "/admin/accounts");
        var browser = new HttpBrowser(port);
        browser.login(email, temporary, "/account/password");
        redirect(browser.get("/admin/posts"), "/account/password");
        browser.password(temporary, PASSWORD);
        browser.login(email, PASSWORD, "/admin/posts");
        return browser;
    }
    private long createPost(HttpBrowser browser, String title) throws Exception {
        var result = browser.post("/admin/posts", Map.of("title", title, "content", "테스트 본문", "authorId", "999999"));
        assertThat(result.statusCode()).isEqualTo(302);
        return Long.parseLong(location(result).substring(location(result).lastIndexOf('/') + 1));
    }

    @Test void completeFlowConnectsLoginAccountsRbacPostsPasswordAndLogout() throws Exception {
        var anonymous = new HttpBrowser(port);
        redirect(anonymous.get("/admin/posts"), "/login");
        anonymous.login(ROOT, "wrong-password", "/login?error");
        var root = root();
        var admin = member(root, "admin@example.test", Role.ADMIN);
        var supporter = member(root, "support@example.test", Role.SUPPORTER);
        assertThat(root.get("/admin/accounts").body()).contains("admin@example.test", "support@example.test");
        for (var denied : List.of(admin, supporter)) {
            assertThat(denied.get("/admin/accounts").statusCode()).isEqualTo(403);
            assertThat(denied.post("/admin/accounts", Map.of("email", "forged@example.test",
                    "displayName", "위조", "role", "ADMIN")).statusCode()).isEqualTo(403);
        }
        long adminPost = createPost(admin, "관리자 게시물");
        long supportPost = createPost(supporter, "서포터 게시물");
        assertThat(jdbc.queryForObject("SELECT author_id FROM posts WHERE id = ?", Long.class, supportPost))
                .isEqualTo(accountMapper.findByEmail("support@example.test").id());
        assertThat(admin.get("/admin/posts").body()).contains("관리자 게시물", "서포터 게시물");
        assertThat(supporter.get("/admin/posts").body()).contains("서포터 게시물").doesNotContain("관리자 게시물");
        assertThat(supporter.get("/admin/posts/" + adminPost).statusCode()).isEqualTo(403);
        assertThat(supporter.get("/admin/posts/" + adminPost + "/edit").statusCode()).isEqualTo(403);
        assertThat(supporter.post("/admin/posts/" + adminPost + "/edit",
                Map.of("title", "침범", "content", "침범")).statusCode()).isEqualTo(403);
        assertThat(supporter.post("/admin/posts/" + adminPost + "/delete", Map.of()).statusCode()).isEqualTo(403);
        redirect(supporter.post("/admin/posts/" + supportPost + "/edit",
                Map.of("title", "본인 수정", "content", "수정 본문")), "/admin/posts/" + supportPost);
        // ADMIN's access to another author's post is the current proposal.
        redirect(admin.post("/admin/posts/" + supportPost + "/edit",
                Map.of("title", "관리자 전체 수정", "content", "관리자가 수정")), "/admin/posts/" + supportPost);
        assertThat(supporter.get("/admin/posts/" + supportPost).body()).contains("관리자 전체 수정");
        redirect(supporter.post("/admin/posts/" + supportPost + "/delete", Map.of()), "/admin/posts");
        assertThat(admin.get("/admin/posts/" + supportPost).statusCode()).isEqualTo(404);
        redirect(admin.post("/admin/posts/" + adminPost + "/edit",
                Map.of("title", "관리자 수정", "content", "수정")), "/admin/posts/" + adminPost);
        redirect(admin.post("/admin/posts/" + adminPost + "/delete", Map.of()), "/admin/posts");
        long extra = createPost(supporter, "전체 삭제 검증");
        redirect(admin.post("/admin/posts/" + extra + "/delete", Map.of()), "/admin/posts");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE deleted_at IS NOT NULL", Integer.class)).isEqualTo(3);
        String next = "Test-Next-Password42";
        admin.password(PASSWORD, next);
        redirect(admin.get("/admin/posts"), "/login");
        admin.login("admin@example.test", PASSWORD, "/login?error");
        admin.login("admin@example.test", next, "/admin/posts");
        redirect(admin.post("/logout", Map.of()), "/login?logout");
        redirect(admin.get("/admin/posts"), "/login");
        var stored = accountMapper.findByEmail("admin@example.test");
        assertThat(stored.passwordHash()).startsWith("$2");
        assertThat(encoder.matches(next, stored.passwordHash())).isTrue();
    }

    @Test void creationAndTemporaryPasswordCannotBypassRestrictions() throws Exception {
        var root = root();
        assertThat(root.get("/admin/accounts/new").body()).contains("value=\"ADMIN\"", "value=\"SUPPORTER\"")
                .doesNotContain("value=\"SUPER_ADMIN\"");
        assertThat(root.post("/admin/accounts", Map.of("email", "illegal@example.test",
                "displayName", "불가", "role", "SUPER_ADMIN")).statusCode()).isEqualTo(400);
        assertThat(accountMapper.findByEmail("illegal@example.test")).isNull();
        root.post("/admin/accounts", Map.of("email", "temporary@example.test", "displayName", "임시", "role", "ADMIN"));
        String temporary = root.issued();
        assertThat(accountMapper.findByEmail("temporary@example.test").passwordHash()).isNotEqualTo(temporary);
        var member = new HttpBrowser(port);
        member.login("temporary@example.test", temporary, "/account/password");
        assertThat(member.post("/admin/posts", Map.of("title", "변경 전", "content", "불가")).statusCode()).isEqualTo(403);
        assertThat(member.post("/account/password", Map.of("currentPassword", "wrong", "newPassword", PASSWORD,
                "confirmPassword", PASSWORD)).statusCode()).isEqualTo(400);
        assertThat(member.post("/account/password", Map.of("currentPassword", temporary, "newPassword", "한".repeat(30),
                "confirmPassword", "한".repeat(30))).statusCode()).isEqualTo(400);
        assertThat(root.post("/admin/accounts", Map.of("email", "TEMPORARY@example.test",
                "displayName", "중복", "role", "SUPPORTER")).statusCode()).isEqualTo(400);
        accounts.bootstrap("another@example.test", "중복 초기화", "Test-Other-Password42");
        assertThat(accountMapper.count()).isEqualTo(2);
    }

    @Test void accountChangesInvalidateExistingSessionsAndUseSeparatePromotion() throws Exception {
        var root = root();
        var member = member(root, "change@example.test", Role.ADMIN);
        long id = accountMapper.findByEmail("change@example.test").id();
        assertThat(root.get("/admin/accounts/" + id + "/role").body()).contains("SUPER_ADMIN");
        redirect(root.post("/admin/accounts/" + id + "/role", Map.of("role", "SUPPORTER")), "/admin/accounts");
        redirect(member.get("/admin/posts"), "/login?expired");
        member.login("change@example.test", PASSWORD, "/admin/posts");
        assertThat(member.get("/admin/posts").body()).contains("본인이 작성한");
        redirect(root.post("/admin/accounts/" + id + "/role", Map.of("role", "SUPER_ADMIN")), "/admin/accounts");
        redirect(member.get("/admin/accounts"), "/login?expired");
        member.login("change@example.test", PASSWORD, "/admin/posts");
        assertThat(member.get("/admin/accounts").statusCode()).isEqualTo(200);
        assertThat(member.post("/admin/accounts/" + id + "/role", Map.of("role", "ADMIN")).statusCode()).isEqualTo(400);
        assertThat(member.post("/admin/accounts/" + id + "/deactivate", Map.of()).statusCode()).isEqualTo(400);
        redirect(root.post("/admin/accounts/" + id + "/reset-password", Map.of()), "/admin/accounts/issued");
        String reset = root.issued();
        redirect(member.get("/admin/posts"), "/login?expired");
        member.login("change@example.test", PASSWORD, "/login?error");
        member.login("change@example.test", reset, "/account/password");
        member.password(reset, "Test-Reset-Password42");
        member.login("change@example.test", "Test-Reset-Password42", "/admin/posts");
        redirect(root.post("/admin/accounts/" + id + "/deactivate", Map.of()), "/admin/accounts");
        redirect(member.get("/admin/posts"), "/login?expired");
        member.login("change@example.test", "Test-Reset-Password42", "/login?error");
    }

    @Test void csrfEscapingValidationAndDeletedRowsAreProtected() throws Exception {
        var root = root();
        assertThat(root.rawPost("/admin/posts", Map.of("title", "CSRF", "content", "blocked")).statusCode()).isEqualTo(403);
        assertThat(root.rawPost("/logout", Map.of()).statusCode()).isEqualTo(403);
        assertThat(root.get("/admin/posts").statusCode()).isEqualTo(200);
        assertThat(root.post("/admin/posts", Map.of("title", "x".repeat(201), "content", "body")).statusCode()).isEqualTo(400);
        assertThat(root.get("/admin/posts?page=-1").statusCode()).isEqualTo(400);
        var created = root.post("/admin/posts", Map.of("title", "<script>title</script>", "content", "<script>alert(1)</script>"));
        String path = java.net.URI.create(location(created)).getRawPath();
        var html = root.get(path);
        assertThat(html.statusCode()).isEqualTo(200);
        assertThat(html.body()).contains("&lt;script&gt;").doesNotContain("<script>");
        assertThat(root.get(path + "/delete").statusCode()).isEqualTo(405);
        assertThat(root.get(path).statusCode()).isEqualTo(200);
        redirect(root.post(path + "/delete", Map.of()), "/admin/posts");
        assertThat(root.get(path).statusCode()).isEqualTo(404);
        assertThat(root.post(path + "/edit", Map.of("title", "복구 시도", "content", "불가")).statusCode()).isEqualTo(404);
        assertThat(root.get("/__stage0/status").statusCode()).isEqualTo(403);
    }

    @Test void supporterPaginationAndServiceAuthorizationUseTheSameScope() throws Exception {
        var root = root();
        var supporter = member(root, "pages@example.test", Role.SUPPORTER);
        var supportPrincipal = principal("pages@example.test");
        for (int i = 0; i < 12; i++) posts.create(supportPrincipal, "own-" + i, "본인 글");
        for (int i = 0; i < 3; i++) posts.create(principal(ROOT), "other-" + i, "다른 글");
        var page0 = posts.list(supportPrincipal, 0);
        var page1 = posts.list(supportPrincipal, 1);
        assertThat(page0.total()).isEqualTo(12);
        assertThat(page0.items()).hasSize(10);
        assertThat(page1.items()).hasSize(2);
        assertThat(supporter.get("/admin/posts?page=1").body()).contains("총 12건").doesNotContain("other-");
        assertThatThrownBy(() -> accounts.list(supportPrincipal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> accounts.create(supportPrincipal, "escape@example.test", "불가", Role.ADMIN))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(accountMapper.findByEmail("escape@example.test")).isNull();
    }

    @Test void concurrentSuperAdminDemotionsCannotRemoveTheLastActiveSuper() throws Exception {
        root();
        var first = principal(ROOT);
        var issued = accounts.create(first, "second@example.test", "두 번째 관리자", Role.ADMIN);
        accounts.changeOwnPassword(principal(issued.email()), issued.temporaryPassword(), PASSWORD, PASSWORD);
        accounts.changeRole(first, issued.accountId(), Role.SUPER_ADMIN);
        var second = principal(issued.email());
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var tasks = List.of(
                pool.submit(() -> attemptDemotion(start, first, second.getId())),
                pool.submit(() -> attemptDemotion(start, second, first.getId())));
            start.countDown();
            int successes = 0;
            for (var task : tasks) if (task.get(15, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
            assertThat(accountMapper.activeSuperCount()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    private boolean attemptDemotion(CountDownLatch start, AccountPrincipal actor, long target) throws InterruptedException {
        start.await();
        try { accounts.changeRole(actor, target, Role.ADMIN); return true; }
        catch (org.springframework.security.access.AccessDeniedException | egovframework.backoffice.mvp.common.BusinessException expected) { return false; }
    }
}
