package egovframework.backoffice.mvp.security;

import egovframework.backoffice.mvp.account.*;
import java.io.Serializable;
import java.util.Collection;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;

public class AccountPrincipal implements UserDetails, CredentialsContainer, Serializable {
    private final long id;
    private final String email;
    private final String displayName;
    private final Role role;
    private final boolean active;
    private final boolean passwordChangeRequired;
    private final long authVersion;
    private String passwordHash;

    public AccountPrincipal(Account account) {
        id = account.id(); email = account.email(); displayName = account.displayName();
        role = account.role(); active = account.active();
        passwordChangeRequired = account.passwordChangeRequired();
        authVersion = account.authVersion(); passwordHash = account.passwordHash();
    }
    public long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public long getAuthVersion() { return authVersion; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return passwordHash; }
    @Override public boolean isEnabled() { return active; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return AccessPolicy.authorities(role); }
    @Override public void eraseCredentials() { passwordHash = null; }
}
