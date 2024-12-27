package org.example.repositories;

import org.example.model.Exam;
import org.example.model.Task;
import org.example.model.TaskGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class ExamDataRepository {

  private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private final JdbcTemplate jdbc;

  public ExamDataRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Exam> getExamById(int examId) {
    String sql = String.format("""
        SELECT id, title
          FROM exam
          WHERE id = %s
        """, examId);

    RowMapper<Exam> rowMapper = (r, i) ->
        new Exam(
            r.getInt("id"),
            r.getString("title"));

    List<Exam> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 exam was found by id ({})", examId);
    }

    if (list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list.getFirst());
  }

  public List<Exam> getAllExams() {
    String sql = """
        SELECT id, title
          FROM exam
        """;

    RowMapper<Exam> rowMapper = (r, i) ->
        new Exam(
            r.getInt("id"),
            r.getString("title"));

    return jdbc.query(sql, rowMapper);
  }

  public void saveExam(String examTitle) {
    String sql = String.format("""
        INSERT INTO exam(title)
          VALUES('%s')
        """, examTitle);

    jdbc.update(sql);
  }

  public void editExam(int examId, String examTitle) {
    String sql = String.format("""
        UPDATE exam
          SET title = '%s'
          WHERE id = %s
        """, examTitle, examId);

    jdbc.update(sql);
  }

  public void deleteExamById(int examId) {
    String sql = String.format("""
        DELETE FROM exam
          WHERE id = %s
        """, examId);

    jdbc.update(sql);
  }


  public Optional<TaskGroup> getTaskGroupsById(int taskGroupId) {
    String sql = String.format("""
        SELECT id, exam_id, serial_number, title, answer_format
          FROM task_group
          WHERE id = %s
        """, taskGroupId);

    RowMapper<TaskGroup> rowMapper = (r, i) ->
        new TaskGroup(
            r.getInt("id"),
            r.getInt("exam_id"),
            r.getInt("serial_number"),
            r.getString("title"),
            r.getString("answer_format"));

    List<TaskGroup> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 task group was found by id ({})", taskGroupId);
    }

    if (list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list.getFirst());
  }

  public List<TaskGroup> getAllTaskGroupsByExamId(int examId) {
    String sql = String.format("""
        SELECT id, exam_id, serial_number, title, answer_format
          FROM task_group
          WHERE exam_id = %s
          ORDER BY serial_number
        """, examId);

    RowMapper<TaskGroup> rowMapper = (r, i) ->
        new TaskGroup(
            r.getInt("id"),
            r.getInt("exam_id"),
            r.getInt("serial_number"),
            r.getString("title"),
            r.getString("answer_format"));

    return jdbc.query(sql, rowMapper);
  }

  public void saveTaskGroup(int examId, String taskGroupTitle, String taskGroupAnswerFormat) {
    // TODO: add call
    List<TaskGroup> taskGroups = getAllTaskGroupsByExamId(examId);

    String sql = String.format("""
        INSERT INTO task_group(exam_id, serial_number, title, answer_format)
          VALUES('%s', '%s', '%s', '%s')
        """, examId, taskGroups.size() + 1, taskGroupTitle, taskGroupAnswerFormat);

    jdbc.update(sql);
  }

  public void editTaskGroup(int taskGroupId, String taskGroupTitle, String taskGroupAnswerFormat) {
    String sql = String.format("""
        UPDATE task_group
          SET title = '%s',
              answer_format = '%s'
          WHERE id = %s
        """, taskGroupTitle, taskGroupAnswerFormat, taskGroupId);

    jdbc.update(sql);
  }

  public void deleteTaskGroupById(int taskGroupId) {
    String sql = String.format("""
        DELETE FROM task_group
          WHERE id = %s
        """, taskGroupId);

    jdbc.update(sql);
  }


  public Optional<Task> getTaskById(int taskId) {
    String sql = String.format("""
        SELECT id, group_id, statement, image, answer
          FROM task
          WHERE id = %s
        """, taskId);

    RowMapper<Task> rowMapper = (r, i) ->
        new Task(
            r.getInt("id"),
            r.getInt("group_id"),
            r.getString("statement"),
            r.getBytes("image"),
            r.getString("answer"));

    List<Task> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 task was found by id ({})", taskId);
    }

    if (list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list.getFirst());
  }

  public List<Task> getAllTasksByGroupId(int taskGroupId) {
    Optional<TaskGroup> group = getTaskGroupsById(taskGroupId);

    if (group.isEmpty()) {
      return List.of();
    }

    String sql = String.format("""
        SELECT id, group_id, statement, image, answer
          FROM task
          WHERE group_id = %s
        """, taskGroupId);

    RowMapper<Task> rowMapper = (r, i) ->
        new Task(
            r.getInt("id"),
            r.getInt("group_id"),
            r.getString("statement"),
            r.getBytes("image"),
            r.getString("answer"));

    return jdbc.query(sql, rowMapper);
  }

  public void saveTask(int taskGroupId, String statement, byte[] image, String answer) {
    // TODO: add call
    String sql = """
        INSERT INTO task(group_id, statement, image, answer)
          VALUES(?, ?, ?, ?)
        """;

    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      ps.setInt(1, taskGroupId);
      ps.setString(2, statement);
      ps.setBytes(3, image);
      ps.setString(4, answer);
      return ps;
    });
  }

  public void editTask(int taskId, String statement, byte[] image, String answer) {
    // TODO: add call
    String sql = """
        UPDATE task
          SET statement = ?,
              image = ?,
              answer = ?
          WHERE id = ?
        """;

    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, statement);
      ps.setBytes(2, image);
      ps.setString(3, answer);
      ps.setInt(4, taskId);
      return ps;
    });
  }

  public void deleteTaskById(int taskId) {
    String sql = String.format("""
          DELETE FROM task
          WHERE id = %s
        """, taskId);

    jdbc.update(sql);
  }

}