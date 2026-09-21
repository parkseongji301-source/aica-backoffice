package egovframework.backoffice.stage0;

import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("stage0")
public class Stage0Controller {
    @GetMapping("/__stage0/status")
    String status(Model model) {
        model.addAttribute("status", "STAGE0_BOOTSTRAP_OK");
        return "stage0/status";
    }
}
