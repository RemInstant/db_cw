package org.example.repositories;

import org.example.model.Subject;
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

  public Optional<Subject> getSubjectById(int subjectId) {
    String sql = String.format("""
        SELECT id, title
          FROM subject
          WHERE id = %s
        """, subjectId);

    RowMapper<Subject> rowMapper = (r, i) ->
        new Subject(
            r.getInt("id"),
            r.getString("title"));

    List<Subject> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 subject was found by id ({})", subjectId);
    }

    if (list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list.getFirst());
  }

  public List<Subject> getAllSubjects() {
    String sql = """
        SELECT id, title
          FROM subject
        """;

    RowMapper<Subject> rowMapper = (r, i) ->
        new Subject(
            r.getInt("id"),
            r.getString("title"));

    return jdbc.query(sql, rowMapper);
  }

  public void saveSubject(String subjectTitle) {
    String sql = String.format("""
        INSERT INTO subject(title)
          VALUES('%s')
        """, subjectTitle);

    jdbc.update(sql);
  }

  public void editSubject(int subjectId, String subjectTitle) {
    String sql = String.format("""
        UPDATE subject
          SET title = '%s'
          WHERE id = %s
        """, subjectTitle, subjectId);

    jdbc.update(sql);
  }

  public void deleteSubjectById(int subjectId) {
    String sql = String.format("""
        DELETE FROM subject
          WHERE id = %s
        """, subjectId);

    jdbc.update(sql);
  }


  public Optional<TaskGroup> getTaskGroupsById(int taskGroupId) {
    String sql = String.format("""
        SELECT id, subject_id, serial_number, title, answer_format
          FROM task_group
          WHERE id = %s
        """, taskGroupId);

    RowMapper<TaskGroup> rowMapper = (r, i) ->
        new TaskGroup(
            r.getInt("id"),
            r.getInt("subject_id"),
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

  public List<TaskGroup> getAllTaskGroupsBySubjectId(int subjectId) {
    String sql = String.format("""
        SELECT id, subject_id, serial_number, title, answer_format
          FROM task_group
          WHERE subject_id = %s
          ORDER BY serial_number
        """, subjectId);

    RowMapper<TaskGroup> rowMapper = (r, i) ->
        new TaskGroup(
            r.getInt("id"),
            r.getInt("subject_id"),
            r.getInt("serial_number"),
            r.getString("title"),
            r.getString("answer_format"));

    return jdbc.query(sql, rowMapper);
  }

  public void saveTaskGroup(int subjectId, String taskGroupTitle, String taskGroupAnswerFormat) {
    // TODO: add call
    List<TaskGroup> taskGroups = getAllTaskGroupsBySubjectId(subjectId);

    String sql = String.format("""
        INSERT INTO task_group(subject_id, serial_number, title, answer_format)
          VALUES('%s', '%s', '%s', '%s')
        """, subjectId, taskGroups.size() + 1, taskGroupTitle, taskGroupAnswerFormat);

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