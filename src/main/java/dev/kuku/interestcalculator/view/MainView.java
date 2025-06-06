package dev.kuku.interestcalculator.view;

import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainView {
    private final ContentDb contentDb;
    @GetMapping("/")
    public String mainView() {
        return "index";
    }

    @GetMapping("/interact")
    public String showInteractPage(Model model) {
        List<ContentDb.ContentRow> contents = contentDb.getAllContents();
        model.addAttribute("contents", contents);
        return "contents"; // This should match your JTE template filename
    }
}
