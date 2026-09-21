package egovframework.backoffice.mvp.account;

import java.io.Serializable;

/** Short-lived, one-time flash attribute. Never persist or log the raw password. */
public record IssuedCredential(long accountId, String email, String temporaryPassword) implements Serializable {
    @Override public String toString() { return "IssuedCredential[accountId=" + accountId + "]"; }
}
