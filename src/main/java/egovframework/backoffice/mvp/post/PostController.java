package egovframework.backoffice.mvp.post;

import egovframework.backoffice.mvp.common.BusinessException;
import egovframework.backoffice.mvp.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/posts")
public class PostController {
    private final PostService posts;
    public PostController(PostService posts) { this.posts = posts; }

    @GetMapping
    public String list(@AuthenticationPrincipal AccountPrincipal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("pageData", posts.list(principal, page));
        return "posts/list";
    }
    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable long id, Model model) {
        model.addAttribute("post", posts.get(principal, id));
        return "posts/detail";
    }
    @GetMapping("/new")
    public String createForm(Model model) { model.addAttribute("editing", false); return "posts/form"; }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable long id, Model model) {
        var post = posts.get(principal, id);
        model.addAttribute("editing", true); model.addAttribute("postId", id);
        model.addAttribute("title", post.title()); model.addAttribute("content", post.content());
        return "posts/form";
    }
    @PostMapping
    public String create(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam String title, @RequestParam String content, Model model, HttpServletResponse response) {
        try {
            return "redirect:/admin/posts/" + posts.create(principal, title, content);
        } catch (BusinessException error) {
            formError(model, response, error, title, content);
            model.addAttribute("editing", false);
            return "posts/form";
        }
    }
    @PostMapping("/{id}/edit")
    public String edit(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable long id,
            @RequestParam String title, @RequestParam String content, Model model, HttpServletResponse response) {
        try {
            posts.edit(principal, id, title, content);
            return "redirect:/admin/posts/" + id;
        } catch (BusinessException error) {
            formError(model, response, error, title, content);
            model.addAttribute("editing", true); model.addAttribute("postId", id);
            return "posts/form";
        }
    }
    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable long id, RedirectAttributes redirect) {
        posts.delete(principal, id);
        redirect.addFlashAttribute("notice", "게시물을 삭제했습니다.");
        return "redirect:/admin/posts";
    }
    private void formError(Model model, HttpServletResponse response, BusinessException error, String title, String content) {
        response.setStatus(400);
        model.addAttribute("formError", error.getMessage());
        model.addAttribute("title", title); model.addAttribute("content", content);
    }
}
