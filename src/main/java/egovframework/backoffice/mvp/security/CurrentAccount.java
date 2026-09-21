package egovframework.backoffice.mvp.security;

import egovframework.backoffice.mvp.account.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccount {
    private final AccountMapper accounts;
    public CurrentAccount(AccountMapper accounts) { this.accounts = accounts; }
    public Account require(AccountPrincipal principal, boolean allowTemporaryPassword) {
        if (principal == null) throw new AccessDeniedException("로그인이 필요합니다.");
        var account = accounts.findById(principal.getId());
        if (account == null || !account.active() || account.authVersion() != principal.getAuthVersion())
            throw new AccessDeniedException("로그인 정보가 변경되었습니다. 다시 로그인하세요.");
        if (!allowTemporaryPassword && account.passwordChangeRequired())
            throw new AccessDeniedException("먼저 임시 비밀번호를 변경하세요.");
        return account;
    }
}
