package egovframework.backoffice.mvp.post;

import java.util.*;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PostMapper extends EgovAbstractMapper {
    public Post find(long id) { return selectOne("Post.find", id); }
    public List<Post> list(Long authorId, int limit, int offset) {
        var values = new HashMap<String, Object>();
        values.put("authorId", authorId); values.put("limit", limit); values.put("offset", offset);
        return selectList("Post.list", values);
    }
    public long count(Long authorId) {
        var values = new HashMap<String, Object>(); values.put("authorId", authorId);
        return selectOne("Post.count", values);
    }
    public long create(String title, String content, long authorId) {
        var values = new HashMap<String, Object>();
        values.put("title", title); values.put("content", content); values.put("authorId", authorId);
        insert("Post.create", values);
        return ((Number) values.get("id")).longValue();
    }
    public int edit(long id, String title, String content, Long authorId) {
        var values = new HashMap<String, Object>();
        values.put("id", id); values.put("title", title); values.put("content", content); values.put("authorId", authorId);
        return update("Post.edit", values);
    }
    public int softDelete(long id, Long authorId) {
        var values = new HashMap<String, Object>(); values.put("id", id); values.put("authorId", authorId);
        return update("Post.softDelete", values);
    }
}
