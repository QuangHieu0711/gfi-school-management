ALTER TABLE IF EXISTS school_years
    DROP CONSTRAINT IF EXISTS school_years_status_check;

ALTER TABLE IF EXISTS semesters
    DROP CONSTRAINT IF EXISTS semesters_status_check;

ALTER TABLE IF EXISTS grade_levels
    DROP CONSTRAINT IF EXISTS grade_levels_status_check;

ALTER TABLE IF EXISTS classes
    DROP CONSTRAINT IF EXISTS classes_status_check;

ALTER TABLE IF EXISTS grade_level_subjects
    DROP CONSTRAINT IF EXISTS grade_level_subjects_status_check;

ALTER TABLE IF EXISTS classes
    DROP COLUMN IF EXISTS is_current;

CREATE TABLE IF NOT EXISTS grade_level_subjects (
    id bigserial PRIMARY KEY,
    grade_level_id bigint NOT NULL,
    subject_id bigint NOT NULL,
    status integer NOT NULL DEFAULT 1,
    description varchar(500),
    created_at timestamp,
    created_by varchar(255),
    updated_at timestamp,
    updated_by varchar(255),
    CONSTRAINT fk_grade_level_subjects_grade_levels FOREIGN KEY (grade_level_id) REFERENCES grade_levels (id),
    CONSTRAINT fk_grade_level_subjects_subjects FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT uk_grade_level_subjects_grade_level_subject UNIQUE (grade_level_id, subject_id)
);

ALTER TABLE IF EXISTS school_years
    ALTER COLUMN status TYPE integer
    USING (
        CASE status::text
            WHEN 'PLANNING' THEN 0
            WHEN 'ACTIVE' THEN 1
            WHEN 'CLOSED' THEN 2
            ELSE status::integer
        END
    );

ALTER TABLE IF EXISTS semesters
    ALTER COLUMN status TYPE integer
    USING (
        CASE status::text
            WHEN 'PLANNING' THEN 0
            WHEN 'ACTIVE' THEN 1
            WHEN 'CLOSED' THEN 2
            ELSE status::integer
        END
    );

ALTER TABLE IF EXISTS grade_levels
    ALTER COLUMN status TYPE integer
    USING (
        CASE status::text
            WHEN 'PLANNING' THEN 0
            WHEN 'ACTIVE' THEN 1
            WHEN 'CLOSED' THEN 2
            ELSE status::integer
        END
    );

ALTER TABLE IF EXISTS classes
    ALTER COLUMN status TYPE integer
    USING (
        CASE status::text
            WHEN 'PLANNING' THEN 0
            WHEN 'ACTIVE' THEN 1
            WHEN 'CLOSED' THEN 2
            ELSE status::integer
        END
    );

ALTER TABLE IF EXISTS grade_level_subjects
    ALTER COLUMN status TYPE integer
    USING (
        CASE status::text
            WHEN 'PLANNING' THEN 0
            WHEN 'ACTIVE' THEN 1
            WHEN 'CLOSED' THEN 2
            ELSE status::integer
        END
    );

ALTER TABLE IF EXISTS school_years
    ADD CONSTRAINT school_years_status_check
    CHECK (status IN (0, 1, 2));

ALTER TABLE IF EXISTS semesters
    ADD CONSTRAINT semesters_status_check
    CHECK (status IN (0, 1, 2));

ALTER TABLE IF EXISTS grade_levels
    ADD CONSTRAINT grade_levels_status_check
    CHECK (status IN (0, 1, 2));

ALTER TABLE IF EXISTS classes
    ADD CONSTRAINT classes_status_check
    CHECK (status IN (0, 1, 2));

ALTER TABLE IF EXISTS grade_level_subjects
    ADD CONSTRAINT grade_level_subjects_status_check
    CHECK (status IN (0, 1, 2));

ALTER TABLE IF EXISTS roles
    ADD COLUMN IF NOT EXISTS code varchar(50);

CREATE UNIQUE INDEX IF NOT EXISTS uk_roles_code
    ON roles (code);

INSERT INTO grade_levels (code, name, grade_number, status, description, created_at, created_by)
SELECT 'KHOI_1', 'Khoi 1', 1, 1, null, NOW(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM grade_levels WHERE code = 'KHOI_1');

INSERT INTO grade_levels (code, name, grade_number, status, description, created_at, created_by)
SELECT 'KHOI_2', 'Khoi 2', 2, 1, null, NOW(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM grade_levels WHERE code = 'KHOI_2');

INSERT INTO grade_levels (code, name, grade_number, status, description, created_at, created_by)
SELECT 'KHOI_3', 'Khoi 3', 3, 1, null, NOW(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM grade_levels WHERE code = 'KHOI_3');

INSERT INTO grade_levels (code, name, grade_number, status, description, created_at, created_by)
SELECT 'KHOI_4', 'Khoi 4', 4, 1, null, NOW(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM grade_levels WHERE code = 'KHOI_4');

INSERT INTO grade_levels (code, name, grade_number, status, description, created_at, created_by)
SELECT 'KHOI_5', 'Khoi 5', 5, 1, null, NOW(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM grade_levels WHERE code = 'KHOI_5');

INSERT INTO classes (code, name, unit_id, grade_level_id, school_year_id, status, description, created_at, created_by)
SELECT v.code, v.name, u.id, gl.id, sy.id, 1, v.description, NOW(), 'SYSTEM'
FROM (VALUES
    ('1A', 'Lớp 1A', 1, 'Lớp 1A năm học 2022 - 2023'),
    ('1B', 'Lớp 1B', 1, 'Lớp 1B năm học 2022 - 2023'),
    ('1C', 'Lớp 1C', 1, 'Lớp 1C năm học 2022 - 2023'),
    ('2A', 'Lớp 2A', 2, 'Lớp 2A năm học 2022 - 2023'),
    ('2B', 'Lớp 2B', 2, 'Lớp 2B năm học 2022 - 2023'),
    ('2C', 'Lớp 2C', 2, 'Lớp 2C năm học 2022 - 2023'),
    ('3A', 'Lớp 3A', 3, 'Lớp 3A năm học 2022 - 2023'),
    ('3B', 'Lớp 3B', 3, 'Lớp 3B năm học 2022 - 2023'),
    ('3C', 'Lớp 3C', 3, 'Lớp 3C năm học 2022 - 2023'),
    ('4C', 'Lớp 4C', 4, 'Lớp 4C năm học 2022 - 2023'),
    ('5A', 'Lớp 5A', 5, 'Lớp 5A năm học 2022 - 2023'),
    ('5B', 'Lớp 5B', 5, 'Lớp 5B năm học 2022 - 2023'),
    ('5C', 'Lớp 5C', 5, 'Lớp 5C năm học 2022 - 2023')
) AS v(code, name, grade_number, description)
JOIN grade_levels gl ON gl.grade_number = v.grade_number
JOIN units u ON u.id = 1
JOIN school_years sy ON sy.id = 1
WHERE NOT EXISTS (
    SELECT 1
    FROM classes c
    WHERE c.unit_id = u.id
      AND c.grade_level_id = gl.id
      AND c.school_year_id = sy.id
      AND c.code = v.code
);
