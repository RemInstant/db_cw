package org.example.repositories;

import org.apache.catalina.User;
import org.example.model.Subject;
import org.example.model.Task;
import org.example.model.TaskGroup;
import org.example.model.UserExamStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
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
        SELECT id, user_id, subject_title, tasks_count, correct_tasks_count
          FROM user_exam_stat
          WHERE id = %s
        """, userExamStatId);

    RowMapper<UserExamStat> rowMapper = (r, i) ->
        new UserExamStat(
            r.getInt("id"),
            r.getInt("user_id"),
            r.getString("subject_title"),
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
        SELECT id, user_id, subject_title, tasks_count, correct_tasks_count
          FROM user_exam_stat
          WHERE user_id = %s
        """, userId);

    RowMapper<UserExamStat> rowMapper = (r, i) ->
        new UserExamStat(
            r.getInt("id"),
            r.getInt("user_id"),
            r.getString("subject_title"),
            r.getInt("tasks_count"),
            r.getInt("correct_tasks_count"));

    return jdbc.query(sql, rowMapper);
  }

  public void saveUserExam(long userId, String subjectTitle,
                           List<Integer> taskIds, List<Boolean> taskCorrectness) {
    String sql = "CALL create_user_exam(?, ?, ?, ?)";

    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql);
      ps.setLong(1, userId);
      ps.setString(2, subjectTitle);
      ps.setArray(3, con.createArrayOf("INT", taskIds.toArray()));
      ps.setArray(4, con.createArrayOf("BOOLEAN", taskCorrectness.toArray()));
      return ps;
    });
  }
}