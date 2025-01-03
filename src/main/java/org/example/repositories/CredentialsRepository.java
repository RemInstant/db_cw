package org.example.repositories;

import org.example.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CredentialsRepository {

  private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private final JdbcTemplate jdbc;

  public CredentialsRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<AppUser> getUserByUsername(String username) {
    String sql = String.format("""
        SELECT app_user.id as id, username, password, ARRAY_AGG(authority) authorities,
               nickname, app_user.registration_date
          FROM user_credentials
          JOIN app_user
            ON user_credentials.user_id = app_user.id
          LEFT JOIN user_authorities
            ON user_authorities.user_id = user_credentials.user_id
          LEFT JOIN user_authority
            ON user_authorities.authority_id = user_authority.id
          WHERE username = '%s'
          GROUP BY app_user.id, username, password, nickname, app_user.registration_date
        """, username);

    RowMapper<AppUser> rowMapper = (r, i) ->
        new AppUser(
            r.getInt("id"),
            r.getString("username"),
            r.getString("password"),
            (String[]) r.getArray("authorities").getArray(),
            r.getTimestamp("registration_date").toLocalDateTime());

    List<AppUser> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 user was found by username ({})", username);
    }

    if (list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list.getFirst());
  }

  public boolean isUserPresentByUsername(String username) {
    String sql = String.format("""
        SELECT username
          FROM user_credentials
          WHERE username = '%s'
        """, username);

    RowMapper<String> rowMapper = (r, i) ->
        r.getString("username");

    List<String> list = jdbc.query(sql, rowMapper);

    if (list.size() > 1) {
      log.warn("More than 1 user was found by username ({})", username);
    }

    return !list.isEmpty();
  }

  public void saveUser(String username, String password) {
    String sql = String.format("""
        CALL create_user('%s', '%s');
        """, username, password);

    jdbc.update(sql);
  }
}