CREATE TABLE IF NOT EXISTS "app_user" (
    "id" BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "nickname" VARCHAR(20) NOT NULL,
    "registration_date" TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS "user_credentials" (
    "user_id" BIGINT PRIMARY KEY REFERENCES "app_user"("id") ON DELETE CASCADE,
    "username" VARCHAR(20) NOT NULL UNIQUE,
    "password" TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS "user_authority" (
    "id" INT PRIMARY KEY,
    "authority" VARCHAR(64) NOT NULL UNIQUE
);


CREATE TABLE IF NOT EXISTS "user_authorities" (
    "user_id" BIGINT NOT NULL REFERENCES "user_credentials"("user_id") ON DELETE CASCADE,
    "authority_id" INT NOT NULL REFERENCES "user_authority"("id") ON DELETE CASCADE,
    PRIMARY KEY ("user_id", "authority_id")
);


CREATE TABLE IF NOT EXISTS "exam" (
    "id" INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "title" VARCHAR(64) NOT NULL UNIQUE
);


CREATE TABLE IF NOT EXISTS "task_group" (
    "id" INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "exam_id" INT NOT NULL REFERENCES "exam"("id") ON DELETE CASCADE,
    "serial_number" INT NOT NULL,
    "title" VARCHAR(100) NOT NULL,
    "answer_format" text NOT NULL,
    UNIQUE ("exam_id", "serial_number")
);


CREATE TABLE IF NOT EXISTS "task" (
    "id" INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "group_id" INT NOT NULL REFERENCES "task_group"("id") ON DELETE CASCADE,
    "statement" TEXT NOT NULL,
    "image" BYTEA,
    "answer" TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS "user_exam" (
    "id" BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "user_id" BIGINT NOT NULL REFERENCES "app_user"("id") ON DELETE CASCADE,
    "exam_title" VARCHAR(64) NOT NULL,
    "submit_date" TIMESTAMP NOT NULL
);


CREATE TABLE IF NOT EXISTS "user_exam_content" (
    "user_exam_id" BIGINT NOT NULL REFERENCES "user_exam"("id") ON DELETE CASCADE,
    "task_id" INT REFERENCES "task"("id") ON DELETE SET NULL,
    PRIMARY KEY ("user_exam_id", "task_id"),
    "correctness" BOOLEAN NOT NULL DEFAULT FALSE
);



-- PROCEDURES

CREATE OR REPLACE PROCEDURE create_user(username VARCHAR, password TEXT)
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
    INSERT INTO "app_user"("nickname", "registration_date")
        VALUES(username, NOW())
        RETURNING "id"
    )
    SELECT "id"
        INTO new_user_id
        FROM "inserted";

    INSERT INTO "user_credentials"("user_id", "username", "password")
        VALUES(new_user_id, username, password);

    INSERT INTO "user_authorities"("user_id", "authority_id")
        VALUES(new_user_id, user_role_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE submit_user_exam(user_id BIGINT, exam_title VARCHAR, task_ids INT[], answers TEXT[])
AS $$
DECLARE
    new_user_exam_id BIGINT;
    iter INT := 1;
    answer_tmp TEXT;
BEGIN
    WITH "inserted" AS (
    INSERT INTO "user_exam"("user_id", "exam_title", "submit_date")
        VALUES(user_id, exam_title, NOW())
        RETURNING "id"
    )
    SELECT "id"
        INTO new_user_exam_id
        FROM "inserted";

    LOOP
        IF iter > array_length(task_ids, 1) THEN
            EXIT;
        END IF;

        IF answers[iter] IS NULL THEN
            INSERT INTO "user_exam_content"("user_exam_id", "task_id", "correctness")
                VALUES (new_user_exam_id, task_ids[iter], FALSE);
        ELSE
            SELECT "answer"
                INTO answer_tmp
                FROM "task"
                WHERE id = task_ids[iter];

            RAISE NOTICE '% = %?', answers[iter], answer_tmp;

            INSERT INTO "user_exam_content"("user_exam_id", "task_id", "correctness")
                VALUES (new_user_exam_id, task_ids[iter], answers[iter] = answer_tmp);
        END IF;

        iter := iter + 1;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- FUNCTIONS

CREATE OR REPLACE FUNCTION generate_exam(input_exam_id INT)
RETURNS TABLE("id" INT, "serial_number" INT, "statement" TEXT, "image" BYTEA, "answer_format" TEXT, "answer" TEXT)
AS $$
BEGIN
    RETURN QUERY
    WITH "rand_task" AS (
        SELECT
                "task"."id" AS "id",
                "task"."group_id" AS "group_id",
                "task_group"."serial_number",
                "task"."statement",
                "task"."image",
                "task_group"."answer_format",
                "task"."answer"
            FROM "task"
            RIGHT JOIN "task_group"
                ON "task"."group_id" = "task_group"."id"
            JOIN "exam"
                ON "task_group"."exam_id" = "exam"."id"
            WHERE "exam"."id" = input_exam_id
            ORDER BY random()
    )
    SELECT
            (ARRAY_AGG("rand_task"."id"))[1] AS "id",
            (ARRAY_AGG("rand_task"."serial_number"))[1] AS "serial_number",
            (ARRAY_AGG("rand_task"."statement"))[1] AS "statement",
            (ARRAY_AGG("rand_task"."image"))[1] AS "image",
            (ARRAY_AGG("rand_task"."answer_format"))[1] AS "answer_format",
            (ARRAY_AGG("rand_task"."answer"))[1] AS "answer"
        FROM "rand_task"
        GROUP BY "group_id"
        ORDER BY "serial_number";
END;
$$ LANGUAGE plpgsql;


-- TRIGGERS

CREATE OR REPLACE FUNCTION update_task_group_serial_numbers()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE "task_group"
        SET "serial_number" = "serial_number" - 1
        WHERE "id" > OLD."id";
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER task_group_serial_numbers_update_trigger
AFTER DELETE ON "task_group"
FOR EACH ROW
EXECUTE FUNCTION update_task_group_serial_numbers();


-- VIEWS

CREATE OR REPLACE VIEW "user_exam_stat" AS
SELECT
        "user_exam"."id" AS "id",
        "user_id",
        "exam_title",
        "submit_date",
        COUNT(*) AS "tasks_count",
        COUNT(*) FILTER (WHERE "correctness" = true) AS "correct_tasks_count"
    FROM "user_exam"
    JOIN "user_exam_content"
        ON "user_exam_content"."user_exam_id" = "user_exam"."id"
    GROUP BY "user_exam"."id"
    ORDER BY "submit_date" DESC;