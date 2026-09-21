package egovframework.backoffice.mvp.account;

import java.time.LocalDateTime;

public record Account(long id, String email, String displayName, String passwordHash,
                      Role role, boolean active, boolean passwordChangeRequired,
                      long authVersion, LocalDateTime createdAt, LocalDateTime updatedAt) {
    @Override public String toString() { return "Account[id=" + id + ", role=" + role + "]"; }
}
