package egovframework.backoffice.verification;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.SpringVersion;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class Stage0WebTest {
    @Autowired TestRestTemplate http;
    @Autowired ApplicationContext context;

    @Test
    void requestedRuntimeVersionsAreActuallyLoaded() {
        assertThat(Runtime.version().feature()).isEqualTo(17);
        assertThat(SpringBootVersion.getVersion()).isEqualTo("3.4.5");
        assertThat(SpringVersion.getVersion()).isEqualTo("6.2.6");
        assertThat(SpringSecurityCoreVersion.getVersion()).isEqualTo("6.4.5");
        assertThat(ServerInfo.getServerNumber()).startsWith("10.1.40.");
    }

    @Test
    void embeddedTomcatRendersThymeleaf() {
        var response = http.getForEntity("/__stage0/status", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("STAGE0_BOOTSTRAP_OK");
        assertThat(response.getBody()).doesNotContain("th:text");
    }

    @Test
    void unconfiguredPathsAreDeniedWithoutImplementingLogin() {
        for (var path : new String[] {"/__stage0/protected", "/login", "/admin/accounts", "/admin/posts"}) {
            assertThat(http.getForEntity(path, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
        var filters = context.getBean(SecurityFilterChain.class).getFilters();
        assertThat(filters).noneMatch(f -> f instanceof UsernamePasswordAuthenticationFilter
                || f instanceof BasicAuthenticationFilter);
    }
}
