package org.example.repositories;

import org.example.model.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TestRepo {

  private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private final JdbcTemplate jdbc;

  public TestRepo(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

//  public void storePurchase(Test purchase) {
//    String sql = "INSERT INTO purchase VALUES (NULL, ?, ?)";
//    jdbc.update(sql, purchase.getProduct(), purchase.getPrice());
//  }

  public UserDetails getUserDetailsByUsername(String username) throws UsernameNotFoundException {
    String sql = String.format("""
            SELECT username, password, ARRAY_AGG(authority) authorities
              FROM app_user
              LEFT JOIN app_user_authority
                ON app_user.id = app_user_authority.user_id
              WHERE username = '%s'
              GROUP BY username, password
            """, username);
    String q = """
CREATE OR REPLACE PROCEDURE create_user(username VARCHAR, password TEXT)
LANGUAGE plpgsql
AS $$
DECLARE
    user_role_id INT;
    new_user_id BIGINT;
BEGIN
    SELECT "id"
        INTO user_role_id
        FROM "user_authority"
        WHERE "authority" = 'ROLE_USER';

    WITH "inserted" AS (
    INSERT INTO "app_user"("nickname", "regisration_date")
        VALUES(username, NOW())
        RETURNING "id"
    )
    SELECT "id"
        INTO new_user_id
        FROM "inserted";
    
    INSERT INTO "user_credentials"("user_id", "username", "password")
        VALUES("new_user_id", username, password);
    
    INSERT INTO "user_authorities"("user_id", "authority_id")
        VALUES(new_user_id, user_role_id);
END;
$$;
      """;

    //sql = "SELECT * FROM test_table;";

    log.debug(sql);


    RowMapper<UserDetails> rowMapper = (r, i) ->
            User.builder()
                    .username(r.getString("username"))
                    .password(r.getString("password"))
                    .authorities((String[]) r.getArray("authorities").getArray())
                    .build();

    List<UserDetails> list = jdbc.query(sql, rowMapper);

    if (list.isEmpty()) {
      throw new UsernameNotFoundException("Username %s not found".formatted(username));
    }

    return list.getFirst();

//    return Optional.ofNullable(jdbc.queryForObject(sql, rowMapper))
//            .orElseThrow(() -> new UsernameNotFoundException("Username %s not found".formatted(username)));
  }

  public List<Test> getAll() {
    String sql = "SELECT * FROM test_table";

    RowMapper<Test> TestRowMapper = (r, i) -> {
      Test rowObject = new Test();
      rowObject.setAbobus(r.getInt("abobus"));
      rowObject.setName(r.getString("name"));
      return rowObject;
    };

    return jdbc.query(sql, TestRowMapper);
  }
}