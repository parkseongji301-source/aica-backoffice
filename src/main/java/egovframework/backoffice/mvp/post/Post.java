package egovframework.backoffice.mvp.post;

import java.time.LocalDateTime;

public record Post(long id, String title, String content, long authorId, String authorName,
                   LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {}
