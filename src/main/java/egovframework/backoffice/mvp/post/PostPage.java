package egovframework.backoffice.mvp.post;

import java.util.List;
public record PostPage(List<Post> items, int page, int pageSize, long total) {
    public long totalPages() { return Math.max(1, (total + pageSize - 1) / pageSize); }
}
