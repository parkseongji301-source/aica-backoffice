package egovframework.backoffice.mvp.account;

import java.util.*;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AccountMapper extends EgovAbstractMapper {
    public Account findByEmail(String email) { return selectOne("Account.findByEmail", email); }
    public Account findById(long id) { return selectOne("Account.findById", id); }
    public List<Account> findAll() { return selectList("Account.findAll"); }
    public int count() { return selectOne("Account.count"); }
    public int activeSuperCount() { return selectOne("Account.activeSuperCount"); }
    public void lockChanges() { selectOne("Account.lockChanges"); }
    public long create(String email, String name, String hash, Role role) {
        var values = new HashMap<String, Object>();
        values.put("email", email); values.put("displayName", name);
        values.put("passwordHash", hash); values.put("role", role);
        insert("Account.create", values);
        return ((Number) values.get("id")).longValue();
    }
    public void changePassword(long id, String hash, boolean required) {
        update("Account.changePassword", Map.of("id", id, "hash", hash, "required", required));
    }
    public void changeRole(long id, Role role) { update("Account.changeRole", Map.of("id", id, "role", role)); }
    public void deactivate(long id) { update("Account.deactivate", id); }
}
