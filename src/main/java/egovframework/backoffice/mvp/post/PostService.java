package egovframework.backoffice.mvp.post;

import egovframework.backoffice.mvp.common.*;
import egovframework.backoffice.mvp.security.*;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService extends EgovAbstractServiceImpl {
    private static final int PAGE_SIZE = 10;
    private final PostMapper posts;
    private final CurrentAccount current;
    private final AccessPolicy policy;
    public PostService(PostMapper posts, CurrentAccount current, AccessPolicy policy) {
        this.posts = posts; this.current = current; this.policy = policy;
    }
    @Transactional(readOnly = true)
    public PostPage list(AccountPrincipal principal, int page) {
        var actor = current.require(principal, false);
        policy.requirePostAccess(actor, actor.id());
        if (page < 0 || page > 100000) throw new BusinessException("페이지 번호가 올바르지 않습니다.");
        Long author = policy.canManageAllPosts(actor.role()) ? null : actor.id();
        return new PostPage(posts.list(author, PAGE_SIZE, page * PAGE_SIZE), page, PAGE_SIZE, posts.count(author));
    }
    @Transactional(readOnly = true)
    public Post get(AccountPrincipal principal, long id) {
        var actor = current.require(principal, false);
        var post = required(id);
        policy.requirePostAccess(actor, post.authorId());
        return post;
    }
    @Transactional
    public long create(AccountPrincipal principal, String title, String content) {
        var actor = current.require(principal, false);
        policy.requirePostAccess(actor, actor.id());
        return posts.create(InputRules.text(title, 200, "제목"), InputRules.text(content, 20000, "본문"), actor.id());
    }
    @Transactional
    public void edit(AccountPrincipal principal, long id, String title, String content) {
        var actor = current.require(principal, false);
        var post = required(id);
        policy.requirePostAccess(actor, post.authorId());
        Long author = policy.canManageAllPosts(actor.role()) ? null : actor.id();
        if (posts.edit(id, InputRules.text(title, 200, "제목"), InputRules.text(content, 20000, "본문"), author) != 1)
            throw missing();
    }
    @Transactional
    public void delete(AccountPrincipal principal, long id) {
        var actor = current.require(principal, false);
        var post = required(id);
        policy.requirePostAccess(actor, post.authorId());
        Long author = policy.canManageAllPosts(actor.role()) ? null : actor.id();
        if (posts.softDelete(id, author) != 1) throw missing();
    }
    private Post required(long id) {
        var post = posts.find(id);
        if (post == null) throw missing();
        return post;
    }
    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, processException("post.missing").getMessage());
    }
}
