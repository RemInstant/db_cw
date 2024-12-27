package org.example.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.Subject;
import org.example.model.Task;
import org.example.model.TaskGroup;
import org.example.repositories.ExamDataRepository;
import org.example.services.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminPanelController {

  private static final Logger log = LoggerFactory.getLogger(AdminPanelController.class);

  private final BackupService backupService;
  private final ExamDataRepository examDataRepository;

  public AdminPanelController(BackupService backupService, ExamDataRepository examDataRepository) {
    this.backupService = backupService;
    this.examDataRepository = examDataRepository;
  }

  @GetMapping("/admin")
  public String getAdminPanel(Model model, Principal principal) {
    return "admin/adminPanel.html";
  }


  @GetMapping("/admin/backup")
  public void loadBackup(HttpServletResponse response) {
    String backupPath = backupService.getBackupPath();

    response.setHeader("Content-disposition", "attachment;filename=" + backupPath);
    response.setContentType("application/octet-stream");

    try {
      backupService.loadBackup(response.getOutputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @PostMapping("/admin/backup")
  public String uploadBackup(MultipartFile backupFile) {
    if (backupFile == null) {
      return "redirect:/admin";
    }

    backupService.restoreFromBackupFile(backupFile);

    return "redirect:/admin";
  }


  @GetMapping("/admin/content/edit")
  public String getContentPanel(Model model) {
    List<Subject> subjects = examDataRepository.getAllSubjects();
    model.addAttribute("subjects", subjects);

    return "admin/contentEditor.html";
  }

  @PostMapping("/admin/content/subject/add")
  public String addSubject(Model model, String subjectTitle) {
    try {
      examDataRepository.saveSubject(subjectTitle);
    } catch (DuplicateKeyException e) {
      model.addAttribute("error", "Предмет с таким названием уже существует");
      //does not work with redirect

      log.warn("Attempt to add duplicate subject");
      return "redirect:/admin/content/edit";
    }

    return "redirect:/admin/content/edit";
  }

  @PostMapping("/admin/content/subject/{subjectId}/delete")
  public String deleteSubject(@PathVariable int subjectId) {
    examDataRepository.deleteSubjectById(subjectId);
    return "redirect:/admin/content/edit";
  }


  @GetMapping("/admin/content/subject/{subjectId}/edit")
  public String getSubjectPanel(Model model, @PathVariable int subjectId) {
    Optional<Subject> subject = examDataRepository.getSubjectById(subjectId);
    List<TaskGroup> groups = examDataRepository.getAllTaskGroupsBySubjectId(subjectId);

    if (subject.isEmpty()) {
      log.warn("Attempt to get panel of non-existent subject");
      return "redirect:/admin/content/edit";
    }

    model.addAttribute("subject", subject.get());
    model.addAttribute("groups", groups);

    return "admin/subjectEditor.html";
  }

  @PostMapping("/admin/content/subject/{subjectId}/edit")
  public String editSubject(Model model, @PathVariable int subjectId, String subjectTitle) {
    examDataRepository.editSubject(subjectId, subjectTitle);
    return String.format("redirect:/admin/content/subject/%s/edit", subjectId);
  }

  @PostMapping("/admin/content/subject/{subjectId}/group/add")
  public String addTaskGroup(Model model, @PathVariable int subjectId,
                             String taskGroupTitle, String taskGroupAnswerFormat) {
    try {
      examDataRepository.saveTaskGroup(subjectId, taskGroupTitle, taskGroupAnswerFormat);
    } catch (DuplicateKeyException e) {
      model.addAttribute("error", "Категория с таким названием уже существует");
      //does not work with redirect

      log.warn("Attempt to add duplicate task group");
      return String.format("redirect:/admin/content/subject/%s/edit", subjectId);
    }

    return String.format("redirect:/admin/content/subject/%s/edit", subjectId);
  }

  @PostMapping("/admin/content/subject/{subjectId}/group/{groupId}/delete")
  public String deleteTaskGroup(@PathVariable int subjectId, @PathVariable int groupId) {
    examDataRepository.deleteTaskGroupById(groupId);
    return String.format("redirect:/admin/content/subject/%s/edit", subjectId);
  }


  @GetMapping("/admin/content/subject/group/{groupId}/edit")
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

  @PostMapping("/admin/content/subject/group/{groupId}/edit")
  public String editTaskGroup(@PathVariable int groupId, String taskGroupTitle, String taskGroupAnswerFormat) {
    examDataRepository.editTaskGroup(groupId, taskGroupTitle, taskGroupAnswerFormat);
    return String.format("redirect:/admin/content/subject/group/%s/edit", groupId);
  }

  @PostMapping("/admin/content/subject/group/{groupId}/task/add")
  public String addTask(@PathVariable int groupId, String statement, String answer) {
    examDataRepository.saveTask(groupId, statement, null, answer);
    return String.format("redirect:/admin/content/subject/group/%s/edit", groupId);
  }

  @PostMapping("/admin/content/subject/group/{groupId}/task/{taskId}/delete")
  public String deleteTask(@PathVariable int groupId, @PathVariable int taskId) {
    examDataRepository.deleteTaskById(taskId);
    return String.format("redirect:/admin/content/subject/group/%s/edit", groupId);
  }


  @GetMapping("/admin/content/subject/group/task/{taskId}/edit")
  public String getTaskPanel(Model model, @PathVariable int taskId) {
    Optional<Task> task = examDataRepository.getTaskById(taskId);

    if (task.isEmpty()) {
      log.warn("Attempt to get panel of non-existent task");
      return "redirect:/admin/content/edit";
    }

    model.addAttribute("task", task.get());

    return "admin/taskEditor.html";
  }

  @PostMapping("/admin/content/subject/group/task/{taskId}/edit")
  public String editTask(@PathVariable int taskId, String statement, String answer) {
    examDataRepository.editTask(taskId, statement, null, answer);
    return String.format("redirect:/admin/content/subject/group/task/%s/edit", taskId);
  }

}
