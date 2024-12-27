package org.example.repositories;

import org.example.model.NumberedTask;
import org.example.model.Task;
import org.example.model.UserExamStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserExamRepository {

  private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private final JdbcTemplate jdbc;

  public UserExamRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<UserExamStat> getUserExamStatById(long userExamStatId) {
    String sql = String.format("""
        SELECT id, user_id, exam_title, submit_date, tasks_count, correct_tasks_count
          FROM user_exam_stat
          WHERE id = %s
        """, userExamStatId);

    RowMapper<UserExamStat> rowMapper = (r, i) ->
        new UserExamStat(
            r.getInt("id"),
            r.getInt("user_id"),
            r.getString("exam_title"),
            r.getTimestamp("submitDate").toLocalDateTime(),
            r.getInt("tasks_count"),
            r.getInt("correct_tasks_count"));

    List<UserExamStat> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 user exam was found by id ({})", userExamStatId);
    }

    if (list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list.getFirst());
  }

  public List<UserExamStat> getUserExamStatByUserId(long userId) {
    String sql = String.format("""
        SELECT id, user_id, exam_title, submit_date, tasks_count, correct_tasks_count
          FROM user_exam_stat
          WHERE user_id = %s
        """, userId);

    RowMapper<UserExamStat> rowMapper = (r, i) ->
        new UserExamStat(
            r.getInt("id"),
            r.getInt("user_id"),
            r.getString("exam_title"),
            r.getTimestamp("submit_date").toLocalDateTime(),
            r.getInt("tasks_count"),
            r.getInt("correct_tasks_count"));

    return jdbc.query(sql, rowMapper);
  }

  public List<NumberedTask> generateUserExam(int examId) {
    String sql = String.format("""
        SELECT id, serial_number, statement, image, answer_format, answer
          FROM generate_exam(%s)
        """, examId);

    RowMapper<NumberedTask> rowMapper = (r, i) ->
        new NumberedTask(
            r.getInt("id"),
            r.getInt("serial_number"),
            r.getString("statement"),
            r.getBytes("image"),
            r.getString("answer_format"),
            r.getString("answer"));

    return jdbc.query(sql, rowMapper);
  }

  public void submitUserExam(long userId, String examTitle,
                           List<Integer> taskIds, List<String> taskAnswers) {
    String sql = "CALL submit_user_exam(?, ?, ?, ?)";

    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      ps.setLong(1, userId);
      ps.setString(2, examTitle);
      ps.setArray(3, con.createArrayOf("INT", taskIds.toArray()));
      ps.setArray(4, con.createArrayOf("TEXT", taskAnswers.toArray()));
      return ps;
    });
  }
}