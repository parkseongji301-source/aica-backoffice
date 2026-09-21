package egovframework.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import egovframework.backoffice.mvp.config.MvpConfiguration;

@SpringBootApplication(scanBasePackages = "egovframework.backoffice.stage0",
        exclude = UserDetailsServiceAutoConfiguration.class)
@Import(MvpConfiguration.class)
public class BackofficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackofficeApplication.class, args);
    }
}
