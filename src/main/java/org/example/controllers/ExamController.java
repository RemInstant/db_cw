package org.example.controllers;

import org.example.model.AppUser;
import org.example.model.Exam;
import org.example.model.NumberedTask;
import org.example.repositories.CredentialsRepository;
import org.example.repositories.ExamDataRepository;
import org.example.repositories.UserExamRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class ExamController {

  private final CredentialsRepository credentialsRepository;
  private final ExamDataRepository examDataRepository;
  private final UserExamRepository userExamRepository;

  public ExamController(CredentialsRepository credentialsRepository, ExamDataRepository examDataRepository,
                        UserExamRepository userExamRepository) {
    this.credentialsRepository = credentialsRepository;
    this.examDataRepository = examDataRepository;
    this.userExamRepository = userExamRepository;
  }

  @GetMapping("/exam")
  public String getExamPage(Model model, int examId) {
    List<NumberedTask> numberedTasks = userExamRepository.generateUserExam(examId);
    model.addAttribute("numberedTasks", numberedTasks);
    return "exam.html";
  }

  @PostMapping("/exam")
  public String postExamPage(Integer[] taskIds, String[] answers, Principal principal) {
    System.out.println(answers.length);
    for (var elem : answers) {
      System.out.println(elem);
    }

    // TODO: fix this shit?

    Optional<AppUser> appUser = credentialsRepository.getUserByUsername(principal.getName());
    Optional<Exam> exam = examDataRepository.getTaskById(taskIds[0])
        .flatMap(task -> examDataRepository.getTaskGroupsById(task.groupId()))
        .flatMap(group -> examDataRepository.getExamById(group.examId()));

    if (appUser.isEmpty() || exam.isEmpty()) {
      // TODO: some
      return "redirect:/home";
    }

    userExamRepository.submitUserExam(appUser.get().id(), exam.get().title(),
                                      Arrays.asList(taskIds), Arrays.asList(answers));

    return "redirect:/home";
  }

}
