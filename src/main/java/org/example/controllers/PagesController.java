package org.example.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;

@Controller
public class PagesController {

  @GetMapping("/")
  public String getIndexPage(Model model, Principal principal) {
    return "home.html";
  }

  @GetMapping("/home")
  public String getHomePage(Model model, Principal principal) {
    return "home.html";
  }

}
