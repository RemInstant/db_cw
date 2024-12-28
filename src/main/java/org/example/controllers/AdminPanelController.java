package org.example.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;
import org.apache.coyote.Response;
import org.example.model.Exam;
import org.example.model.Task;
import org.example.model.TaskGroup;
import org.example.repositories.ExamDataRepository;
import org.example.services.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminPanelController {

  private static final Logger log = LoggerFactory.getLogger(AdminPanelController.class);

  private final HttpSession session;
  private final BackupService backupService;
  private final ExamDataRepository examDataRepository;

  public AdminPanelController(HttpSession session, BackupService backupService,
                              ExamDataRepository examDataRepository) {
    this.session = session;
    this.backupService = backupService;
    this.examDataRepository = examDataRepository;
  }

  @GetMapping("/admin")
  public String getAdminPanel(Model model, Principal principal) {
    return "admin/adminPanel.html";
  }


  @GetMapping("/admin/backup")
  public ResponseEntity<?> loadBackup(HttpServletRequest request, HttpServletResponse response,
                                             Model model) {
    try {
      backupService.createBackup();
    } catch (RuntimeException e) {
      log.error("Failed to load backup", e);
      return ResponseEntity.internalServerError()
          .body("При подготовке файла к скачиванию произошла ошибка");
    }

    String backupPath = backupService.getBackupPath();
    Resource resource = new FileSystemResource(backupPath);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=backup.bak")
        .body(resource);
  }

  @PostMapping("/admin/backup")
  public String uploadBackup(HttpServletRequest request, Model model, MultipartFile backupFile) {
    if (backupFile == null) {
      return "redirect:/admin";
    }

    try {
      backupService.restoreFromBackupFile(backupFile);
    } catch (RuntimeException e) {
      model.addAttribute("GLOBAL_EXCEPTION_MESSAGE",
          "При попытке восстановить БД из резервной копии произошла ошибка");
      model.addAttribute("GLOBAL_PREV_PAGE", request.getHeader("Referer"));
      log.error("Failed to upload backup", e);
      return "error.html";
    }

    return "redirect:/admin";
  }


  @GetMapping("/admin/content/edit")
  public String getContentPanel(Model model) {
    List<Exam> exams = examDataRepository.getAllExams();
    model.addAttribute("exams", exams);

    return "admin/contentEditor.html";
  }

  @PostMapping("/admin/content/exam/add")
  public String addExam(HttpServletRequest request, Model model, String examTitle) {
    validateLength(examTitle, 32, "Слишком длинное название");
    try {
      examDataRepository.saveExam(examTitle);
    } catch (DuplicateKeyException e) {
      model.addAttribute("GLOBAL_EXCEPTION_MESSAGE",
          "Экзамен с таким названием уже существует");
      model.addAttribute("GLOBAL_PREV_PAGE", request.getHeader("Referer"));
      log.warn("Attempt to add duplicate exam");
      return "error.html";
    }

    return "redirect:/admin/content/edit";
  }

  @PostMapping("/admin/content/exam/{examId}/delete")
  public String deleteExam(@PathVariable int examId) {
    examDataRepository.deleteExamById(examId);
    return "redirect:/admin/content/edit";
  }


  @GetMapping("/admin/content/exam/{examId}/edit")
  public String getExamPanel(Model model, @PathVariable int examId) {
    Optional<Exam> exam = examDataRepository.getExamById(examId);
    List<TaskGroup> groups = examDataRepository.getAllTaskGroupsByExamId(examId);

    if (exam.isEmpty()) {
      log.warn("Attempt to get panel of non-existent exam");
      return "redirect:/admin/content/edit";
    }

    model.addAttribute("exam", exam.get());
    model.addAttribute("groups", groups);

    return "admin/examEditor.html";
  }

  @PostMapping("/admin/content/exam/{examId}/edit")
  public String editExam(Model model, @PathVariable int examId, String examTitle) {
    validateLength(examTitle, 32, "Слишком длинное название");
    examDataRepository.editExam(examId, examTitle);
    return String.format("redirect:/admin/content/exam/%s/edit", examId);
  }

  @PostMapping("/admin/content/exam/{examId}/group/add")
  public String addTaskGroup(HttpServletRequest request, Model model, @PathVariable int examId,
                             String taskGroupTitle, String taskGroupAnswerFormat) {
    validateLength(taskGroupTitle, 50, "Слишком длинное название");
    try {
      examDataRepository.saveTaskGroup(examId, taskGroupTitle, taskGroupAnswerFormat);
    } catch (DuplicateKeyException e) {
      model.addAttribute("GLOBAL_EXCEPTION_MESSAGE",
          "Категория с таким номером уже существует");
      model.addAttribute("GLOBAL_PREV_PAGE", request.getHeader("Referer"));
      log.warn("Attempt to add duplicate task group");
      return "error.html";
    }

    return String.format("redirect:/admin/content/exam/%s/edit", examId);
  }

  @PostMapping("/admin/content/exam/{examId}/group/{groupId}/delete")
  public String deleteTaskGroup(@PathVariable int examId, @PathVariable int groupId) {
    examDataRepository.deleteTaskGroupById(groupId);
    return String.format("redirect:/admin/content/exam/%s/edit", examId);
  }


  @GetMapping("/admin/content/exam/group/{groupId}/edit")
  public String getTaskGroupPanel(Model model, @PathVariable int groupId) {
    Optional<TaskGroup> group = examDataRepository.getTaskGroupsById(groupId);
    List<Task> tasks = examDataRepository.getAllTasksByGroupId(groupId);

    if (group.isEmpty()) {
      log.warn("Attempt to get panel of non-existent task group");
      return "redirect:/admin/content/edit";
    }

    model.addAttribute("taskGroup", group.get());
    model.addAttribute("tasks", tasks);

    return "admin/taskGroupEditor.html";
  }

  @PostMapping("/admin/content/exam/group/{groupId}/edit")
  public String editTaskGroup(@PathVariable int groupId, String taskGroupTitle, String taskGroupAnswerFormat) {
    validateLength(taskGroupTitle, 50, "Слишком длинное название");
    examDataRepository.editTaskGroup(groupId, taskGroupTitle, taskGroupAnswerFormat);
    return String.format("redirect:/admin/content/exam/group/%s/edit", groupId);
  }

  @PostMapping("/admin/content/exam/group/{groupId}/task/add")
  public String addTask(@PathVariable int groupId, String statement, String answer) {
    examDataRepository.saveTask(groupId, statement, null, answer);
    return String.format("redirect:/admin/content/exam/group/%s/edit", groupId);
  }

  @PostMapping("/admin/content/exam/group/{groupId}/task/{taskId}/delete")
  public String deleteTask(@PathVariable int groupId, @PathVariable int taskId) {
    examDataRepository.deleteTaskById(taskId);
    return String.format("redirect:/admin/content/exam/group/%s/edit", groupId);
  }


  @GetMapping("/admin/content/exam/group/task/{taskId}/edit")
  public String getTaskPanel(Model model, @PathVariable int taskId) {
    Optional<Task> task = examDataRepository.getTaskById(taskId);

    if (task.isEmpty()) {
      log.warn("Attempt to get panel of non-existent task");
      return "redirect:/admin/content/edit";
    }

    model.addAttribute("task", task.get());

    return "admin/taskEditor.html";
  }

  @PostMapping("/admin/content/exam/group/task/{taskId}/edit")
  public String editTask(@PathVariable int taskId, String statement, String answer) {
    examDataRepository.editTask(taskId, statement, null, answer);
    return String.format("redirect:/admin/content/exam/group/task/%s/edit", taskId);
  }

  private void validateLength(String str, int len, String msg) {
    if (str.length() > len) {
      session.setAttribute("GLOBAL_EXCEPTION_MESSAGE", msg);
      throw new RuntimeException("invalid length");
    }
  }
}
