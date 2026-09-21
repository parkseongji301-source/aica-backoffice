package egovframework.backoffice.mvp.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements ApplicationRunner {
    private final AccountService accounts;
    private final boolean enabled;
    private final String email;
    private final String name;
    private final String password;
    public InitialAdminInitializer(AccountService accounts,
            @Value("${backoffice.bootstrap.enabled:false}") boolean enabled,
            @Value("${backoffice.bootstrap.email:}") String email,
            @Value("${backoffice.bootstrap.name:최상위 관리자}") String name,
            @Value("${backoffice.bootstrap.password:}") String password) {
        this.accounts = accounts; this.enabled = enabled; this.email = email; this.name = name; this.password = password;
    }
    @Override public void run(ApplicationArguments args) {
        if (enabled) accounts.bootstrap(email, name, password);
    }
}
