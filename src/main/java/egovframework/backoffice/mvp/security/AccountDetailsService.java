package egovframework.backoffice.mvp.security;

import egovframework.backoffice.mvp.account.AccountMapper;
import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final AccountMapper accounts;
    public AccountDetailsService(AccountMapper accounts) { this.accounts = accounts; }
    @Override public UserDetails loadUserByUsername(String username) {
        var account = accounts.findByEmail(username.strip().toLowerCase(Locale.ROOT));
        if (account == null) throw new UsernameNotFoundException("로그인 정보를 확인하세요.");
        return new AccountPrincipal(account);
    }
}
