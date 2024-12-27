package org.example.controllers;

import org.example.model.AppUser;
import org.example.model.Exam;
import org.example.model.UserExamStat;
import org.example.repositories.CredentialsRepository;
import org.example.repositories.ExamDataRepository;
import org.example.repositories.UserExamRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

  private final CredentialsRepository credentialsRepository;
  private final ExamDataRepository examDataRepository;
  private final UserExamRepository userExamRepository;

  public HomeController(CredentialsRepository credentialsRepository, ExamDataRepository examDataRepository,
                        UserExamRepository userExamRepository) {
    this.credentialsRepository = credentialsRepository;
    this.examDataRepository = examDataRepository;
    this.userExamRepository = userExamRepository;
  }

  @GetMapping("/")
  public String getIndexPage() {
    return "redirect:/home";
  }

  @GetMapping("/home")
  public String getHomePage(Model model, Authentication authentication) {
    List<Exam> exams = examDataRepository.getAllExams();
    model.addAttribute("exams", exams);

    if (authentication != null && authentication.isAuthenticated()) {
      Optional<AppUser> appUser = credentialsRepository.getUserByUsername(authentication.getName());
      if (appUser.isPresent()) {
        List<UserExamStat> userExamStats = userExamRepository.getUserExamStatByUserId(appUser.get().id());
        model.addAttribute("userExamStats", userExamStats);
        model.addAttribute("test", appUser.get().registrationDateTime()
            .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
      }
    }

    return "home.html";
  }

}
