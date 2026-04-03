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

ALTER TABLE IF EXISTS classroom_subjects
    DROP CONSTRAINT IF EXISTS classroom_subjects_status_check;

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

CREATE TABLE IF NOT EXISTS classroom_subjects (
    id bigserial PRIMARY KEY,
    classroom_id bigint NOT NULL,
    subject_id bigint NOT NULL,
    status integer NOT NULL DEFAULT 1,
    description varchar(500),
    created_at timestamp,
    created_by varchar(255),
    updated_at timestamp,
    updated_by varchar(255),
    CONSTRAINT fk_classroom_subjects_classes FOREIGN KEY (classroom_id) REFERENCES classes (id),
    CONSTRAINT fk_classroom_subjects_subjects FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT uk_classroom_subjects_classroom_subject UNIQUE (classroom_id, subject_id)
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

ALTER TABLE IF EXISTS classroom_subjects
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

ALTER TABLE IF EXISTS classroom_subjects
    ADD CONSTRAINT classroom_subjects_status_check
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

CREATE TABLE IF NOT EXISTS students (
    id bigserial PRIMARY KEY,
    student_code varchar(50) NOT NULL UNIQUE,
    full_name varchar(255) NOT NULL,
    first_name varchar(100),
    moe_code varchar(50),
    date_of_birth date NOT NULL,
    gender varchar(20),
    place_of_birth varchar(255),
    ethnicity varchar(100),
    religion varchar(100),
    nationality varchar(100) DEFAULT 'Việt Nam',
    mobile_phone varchar(20),
    email varchar(255),
    identity_number varchar(50),
    identity_issue_date date,
    identity_issue_place varchar(255),
    health_insurance_number varchar(50),
    blood_group varchar(50),
    boarding_book varchar(100),
    admission_date date,
    student_status integer,
    admission_type varchar(100),
    unit_id bigint NOT NULL,
    created_at timestamp,
    created_by varchar(255),
    updated_at timestamp,
    updated_by varchar(255),
    CONSTRAINT fk_students_unit FOREIGN KEY (unit_id) REFERENCES units (id)
);

CREATE TABLE IF NOT EXISTS student_enrollments (
    id bigserial PRIMARY KEY,
    student_id bigint NOT NULL,
    school_year_id bigint NOT NULL,
    class_id bigint NOT NULL,
    enrolled_at date,
    status integer,
    is_repeater boolean DEFAULT false,
    sessions_per_week varchar(50),
    study_mode varchar(50),
    is_boarding boolean DEFAULT false,
    is_two_sessions_per_day boolean DEFAULT false,
    created_at timestamp,
    created_by varchar(255),
    updated_at timestamp,
    updated_by varchar(255),
    CONSTRAINT fk_student_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_student_enrollments_school_year FOREIGN KEY (school_year_id) REFERENCES school_years (id),
    CONSTRAINT fk_student_enrollments_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT uk_student_enrollment UNIQUE (student_id, school_year_id)
);

CREATE TABLE IF NOT EXISTS student_addresses (
    id bigserial PRIMARY KEY,
    student_id bigint NOT NULL,
    address_type varchar(30) NOT NULL,
    province_name varchar(255),
    ward_name varchar(255),
    hamlet_name varchar(255),
    detail_address varchar(500),
    CONSTRAINT fk_student_addresses_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE IF NOT EXISTS student_guardians (
    id bigserial PRIMARY KEY,
    student_id bigint NOT NULL,
    guardian_type varchar(20) NOT NULL,
    full_name varchar(255),
    birth_year integer,
    occupation varchar(255),
    phone varchar(20),
    email varchar(255),
    identity_number varchar(50),
    is_ethnic boolean DEFAULT false,
    CONSTRAINT fk_student_guardians_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE IF NOT EXISTS student_profiles (
    id bigserial PRIMARY KEY,
    student_id bigint NOT NULL UNIQUE,
    policy_object varchar(255),
    policy_benefit varchar(255),
    priority_category varchar(255),
    student_category varchar(255),
    region_category varchar(255),
    disability_type varchar(255),
    disability_exempt_eval boolean DEFAULT false,
    support_tuition_cost boolean DEFAULT false,
    resettlement_area boolean DEFAULT false,
    housing_support boolean DEFAULT false,
    monthly_allowance boolean DEFAULT false,
    rice_support boolean DEFAULT false,
    follows_moe_program boolean DEFAULT true,
    can_swim boolean DEFAULT false,
    learns_ethnic_language boolean DEFAULT false,
    studied_kindergarten_5yo boolean DEFAULT false,
    needs_vietnamese_support boolean DEFAULT false,
    has_vietnamese_reinforcement_material boolean DEFAULT false,
    has_ethnic_teaching_assistant boolean DEFAULT false,
    has_parent_internet boolean DEFAULT false,
    has_parent_smartphone boolean DEFAULT false,
    foreign_language_program varchar(100),
    foreign_language_certificate varchar(255),
    informatics_certificate varchar(255),
    career_orientation varchar(255),
    vocational_orientation varchar(255),
    joined_team_date date,
    joined_union_date date,
    joined_party_date date,
    other_system_code varchar(100),
    sso_code varchar(100),
    CONSTRAINT fk_student_profiles_student FOREIGN KEY (student_id) REFERENCES students (id)
);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS policy_object varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS policy_benefit varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS priority_category varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS student_category varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS region_category varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS disability_type varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS disability_exempt_eval boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS support_tuition_cost boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS resettlement_area boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS housing_support boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS monthly_allowance boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS rice_support boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS follows_moe_program boolean DEFAULT true;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS can_swim boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS learns_ethnic_language boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS studied_kindergarten_5yo boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS needs_vietnamese_support boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS has_vietnamese_reinforcement_material boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS has_ethnic_teaching_assistant boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS has_parent_internet boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS has_parent_smartphone boolean DEFAULT false;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS foreign_language_program varchar(100);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS foreign_language_certificate varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS informatics_certificate varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS career_orientation varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS vocational_orientation varchar(255);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS joined_team_date date;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS joined_union_date date;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS joined_party_date date;

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS other_system_code varchar(100);

ALTER TABLE IF EXISTS student_profiles
    ADD COLUMN IF NOT EXISTS sso_code varchar(100);

INSERT INTO students (
    student_code,
    full_name,
    first_name,
    moe_code,
    date_of_birth,
    gender,
    place_of_birth,
    ethnicity,
    religion,
    nationality,
    mobile_phone,
    email,
    identity_number,
    identity_issue_date,
    identity_issue_place,
    health_insurance_number,
    blood_group,
    boarding_book,
    admission_date,
    student_status,
    admission_type,
    unit_id,
    created_at,
    created_by
)
SELECT
    'HS0001',
    'Nguyễn Lê Minh Anh',
    'Minh Anh',
    'MOE-HS0001',
    DATE '2017-09-12',
    'Nữ',
    'Hà ội',
    'Kinh',
    'Không',
    'Việt Nam',
    '0912345678',
    'minhanh.hs0001@example.com',
    '001207123456',
    DATE '2025-01-15',
    'Công an TP Hà Nội',
    'HS-BHYT-0001',
    'O+',
    'HK-0001',
    DATE '2023-08-20',
    1,
    'Xét tuyển',
    c.unit_id,
    NOW(),
    'SYSTEM'
FROM classes c
WHERE c.code = '1A'
  AND NOT EXISTS (
      SELECT 1
      FROM students s
      WHERE s.student_code = 'HS0001'
  )
ORDER BY c.id
LIMIT 1;

INSERT INTO student_enrollments (
    student_id,
    school_year_id,
    class_id,
    enrolled_at,
    status,
    is_repeater,
    sessions_per_week,
    study_mode,
    is_boarding,
    is_two_sessions_per_day,
    created_at,
    created_by
)
SELECT
    s.id,
    c.school_year_id,
    c.id,
    DATE '2023-08-20',
    1,
    false,
    '10 buổi/tuần',
    'Bán trú',
    false,
    true,
    NOW(),
    'SYSTEM'
FROM students s
JOIN classes c ON c.code = '1A'
WHERE s.student_code = 'HS0001'
  AND NOT EXISTS (
      SELECT 1
      FROM student_enrollments se
      WHERE se.student_id = s.id
        AND se.school_year_id = c.school_year_id
  )
ORDER BY c.id
LIMIT 1;

INSERT INTO student_addresses (
    student_id,
    address_type,
    province_name,
    ward_name,
    hamlet_name,
    detail_address
)
SELECT
    s.id,
    v.address_type,
    v.province_name,
    v.ward_name,
    v.hamlet_name,
    v.detail_address
FROM students s
JOIN (
    VALUES
        ('THUONG_TRU', 'Hà Nội', 'Cầu Giấy', 'Địch Vọng', '12 ngõ 81 Đường Xuân Thủy'),
        ('TAM_TRU', 'Hà Nội', 'Nam Từ Liêm', 'Mỹ Đình 2', 'ăn hộ A12 Chung cư Green Bay')
) AS v(address_type, province_name, ward_name, hamlet_name, detail_address) ON 1 = 1
WHERE s.student_code = 'HS0001'
  AND NOT EXISTS (
      SELECT 1
      FROM student_addresses sa
      WHERE sa.student_id = s.id
  );

INSERT INTO student_guardians (
    student_id,
    guardian_type,
    full_name,
    birth_year,
    occupation,
    phone,
    email,
    identity_number,
    is_ethnic
)
SELECT
    s.id,
    v.guardian_type,
    v.full_name,
    v.birth_year,
    v.occupation,
    v.phone,
    v.email,
    v.identity_number,
    v.is_ethnic
FROM students s
JOIN (
    VALUES
        ('CHA', 'Nguyễn Văn Hưng', 1986, 'Kỹ sư xây dựng', '0901112233', 'hung.ph@example.com', '001086000111', false),
        ('ME', 'Lê Thị Hoa', 1989, 'Kế toán', '0904445566', 'hoa.ph@example.com', '001189000222', false)
) AS v(guardian_type, full_name, birth_year, occupation, phone, email, identity_number, is_ethnic) ON 1 = 1
WHERE s.student_code = 'HS0001'
  AND NOT EXISTS (
      SELECT 1
      FROM student_guardians sg
      WHERE sg.student_id = s.id
  );

INSERT INTO student_profiles (
    student_id,
    policy_object,
    policy_benefit,
    priority_category,
    student_category,
    region_category,
    disability_type,
    disability_exempt_eval,
    support_tuition_cost,
    resettlement_area,
    housing_support,
    monthly_allowance,
    rice_support,
    follows_moe_program,
    can_swim,
    learns_ethnic_language,
    studied_kindergarten_5yo,
    needs_vietnamese_support,
    has_vietnamese_reinforcement_material,
    has_ethnic_teaching_assistant,
    has_parent_internet,
    has_parent_smartphone,
    foreign_language_program,
    foreign_language_certificate,
    informatics_certificate,
    career_orientation,
    vocational_orientation,
    joined_team_date,
    joined_union_date,
    joined_party_date,
    other_system_code,
    sso_code
)
SELECT
    s.id,
    'Không',
    'Không',
    'Ưu tiên khu vực 3',
    'Học sinh tiểu học',
    'Thành thị',
    null,
    false,
    false,
    false,
    false,
    false,
    false,
    true,
    true,
    false,
    true,
    false,
    true,
    false,
    true,
    true,
    'Tiếng Anh tăng cường',
    'Cambridge Starters',
    'Chứng chỉ tin học cơ bản',
    'Chưa áp dụng',
    'Chưa áp dụng',
    DATE '2025-03-26',
    null,
    null,
    'EXT-HS0001',
    'SSO-HS0001'
FROM students s
WHERE s.student_code = 'HS0001'
  AND NOT EXISTS (
      SELECT 1
      FROM student_profiles sp
      WHERE sp.student_id = s.id
  );
