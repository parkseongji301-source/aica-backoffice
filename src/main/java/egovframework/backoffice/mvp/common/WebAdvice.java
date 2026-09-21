package egovframework.backoffice.mvp.common;

import egovframework.backoffice.mvp.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class WebAdvice {
    private final AccessPolicy policy;
    public WebAdvice(AccessPolicy policy) { this.policy = policy; }

    @ModelAttribute
    public void common(Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AccountPrincipal principal) {
            model.addAttribute("currentUser", principal);
            model.addAttribute("canManageAccounts", policy.canManageAccounts(principal.getRole()));
            model.addAttribute("canManageAllPosts", policy.canManageAllPosts(principal.getRole()));
        }
    }
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BusinessException error, Model model) {
        model.addAttribute("errorTitle", "입력 내용을 확인하세요");
        model.addAttribute("errorMessage", error.getMessage());
        return "error";
    }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied(Model model) {
        model.addAttribute("errorTitle", "접근 권한이 없습니다");
        model.addAttribute("errorMessage", "허용된 계정과 게시물 범위에서만 이용할 수 있습니다.");
        return "error";
    }
    @ExceptionHandler(ResponseStatusException.class)
    public String missing(ResponseStatusException error, Model model, jakarta.servlet.http.HttpServletResponse response) {
        response.setStatus(error.getStatusCode().value());
        model.addAttribute("errorTitle", "요청한 항목을 찾을 수 없습니다");
        model.addAttribute("errorMessage", error.getReason());
        return "error";
    }
}
