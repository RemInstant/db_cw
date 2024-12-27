package org.example.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;

@Controller
public class ExamController {

  @GetMapping("/exam")
  public String getExamPage(Model model, Principal principal) {
    return "exam.html";
  }

  @PostMapping("/exam")
  public String postExamPage(String[] questions, Model model, Principal principal) {
    for (var elem : questions) {
      System.out.println(elem);
    }

    return "redirect:/home";
  }

}
