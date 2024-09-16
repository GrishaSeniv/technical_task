package technikal.task.fishmarket.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class IndexController {
    @GetMapping("")
    String redirectToFish() {
        return "redirect:/fish";
    }
}