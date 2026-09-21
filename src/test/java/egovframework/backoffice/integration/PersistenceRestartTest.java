package egovframework.backoffice.integration;

import egovframework.backoffice.BackofficeApplication;
import egovframework.backoffice.mvp.account.*;
import egovframework.backoffice.mvp.post.*;
import egovframework.backoffice.mvp.security.AccountPrincipal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import static egovframework.backoffice.integration.HttpBrowser.*;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenceRestartTest {
    @TempDir Path directory;
    @Test void fileDatabaseSurvivesCompleteServerShutdownAndRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("backoffice").toAbsolutePath().toString().replace('\\', '/')
                + ";DB_CLOSE_ON_EXIT=FALSE";
        long postId;
        String hash;
        String email = "restart@example.test";
        String initial = "Test-Restart-Initial42";
        String changed = "Test-Restart-Changed42";
        try (var first = start(url, true, email, initial)) {
            var mapper = first.getBean(AccountMapper.class);
            var service = first.getBean(AccountService.class);
            var original = mapper.findByEmail(email);
            service.changeOwnPassword(new AccountPrincipal(original), initial, changed, changed);
            var actor = new AccountPrincipal(mapper.findByEmail(email));
            service.create(actor, "persist-admin@example.test", "지속 관리자", Role.ADMIN);
            service.create(actor, "persist-support@example.test", "지속 서포터", Role.SUPPORTER);
            postId = first.getBean(PostService.class).create(actor, "재시작 후 유지", "파일 DB 본문");
            hash = mapper.findByEmail(email).passwordHash();
            var browser = new HttpBrowser(port(first));
            browser.login(email, changed, "/admin/posts");
            assertThat(browser.get("/admin/posts/" + postId).body()).contains("파일 DB 본문");
        }
        // A new application context and a new embedded Tomcat process lifecycle, using the same DB file.
        try (var second = start(url, true, email, initial)) {
            var mapper = second.getBean(AccountMapper.class);
            assertThat(mapper.count()).isEqualTo(3);
            assertThat(mapper.findByEmail(email).passwordHash()).isEqualTo(hash);
            var browser = new HttpBrowser(port(second));
            redirect(browser.get("/admin/posts"), "/login");
            browser.login(email, initial, "/login?error");
            browser.login(email, changed, "/admin/posts");
            assertThat(browser.get("/admin/posts/" + postId).body()).contains("재시작 후 유지", "파일 DB 본문");
            assertThat(browser.get("/admin/accounts").body()).contains("persist-admin@example.test", "persist-support@example.test");
        }
    }
    private ConfigurableApplicationContext start(String url, boolean bootstrap, String email, String password) {
        return new SpringApplicationBuilder(BackofficeApplication.class).profiles("dev").run(
                "--server.port=0", "--spring.datasource.url=" + url,
                "--backoffice.bootstrap.enabled=" + bootstrap,
                "--backoffice.bootstrap.email=" + email,
                "--backoffice.bootstrap.password=" + password);
    }
    private int port(ConfigurableApplicationContext context) {
        return ((ServletWebServerApplicationContext) context).getWebServer().getPort();
    }
}
