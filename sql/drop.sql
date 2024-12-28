DROP PROCEDURE create_user(username VARCHAR, password TEXT);
DROP PROCEDURE submit_user_exam(user_id BIGINT, exam_title VARCHAR, task_ids INT[], answers TEXT[]);

DROP FUNCTION generate_exam(input_exam_id INT);

DROP TRIGGER task_group_serial_numbers_update_trigger ON "task_group";
DROP FUNCTION update_task_group_serial_numbers();

DROP VIEW "user_exam_stat";

DROP TABLE "user_exam_content";
DROP TABLE "user_exam";
DROP TABLE "task";
DROP TABLE "task_group";
DROP TABLE "subject";

DROP TABLE "user_authorities";
DROP TABLE "user_authority";
DROP TABLE "user_credentials";
DROP TABLE "app_user";