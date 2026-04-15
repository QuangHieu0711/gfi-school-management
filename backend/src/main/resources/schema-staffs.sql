-- Bảng lõi: Thông tin cán bộ/giáo viên
CREATE TABLE IF NOT EXISTS staffs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL UNIQUE,
    unit_id BIGINT NOT NULL,
    staff_code VARCHAR(50) NOT NULL UNIQUE,
    identity_code VARCHAR(50) NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    alias_name VARCHAR(255) NULL,
    gender VARCHAR(20) NULL,
    date_of_birth DATE NULL,
    ethnicity_id BIGINT NULL,
    religion_id BIGINT NULL,
    nationality_id BIGINT NULL,
    cccd_no VARCHAR(50) NULL,
    cccd_issue_date DATE NULL,
    cccd_issue_place VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    health_status VARCHAR(255) NULL,
    social_insurance_no VARCHAR(50) NULL,
    avatar_file_id BIGINT NULL,
    signature_file_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    note TEXT NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (unit_id) REFERENCES units(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_unit_id (unit_id),
    INDEX idx_staff_code (staff_code),
    INDEX idx_deleted_flag (deleted_flag)
);

-- Bảng địa chỉ: Hộ khẩu, quê quán, nơi sinh...
CREATE TABLE IF NOT EXISTS staff_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    address_type VARCHAR(30) NOT NULL,
    -- PERMANENT, HOMETOWN, BIRTHPLACE, CURRENT
    province_id BIGINT NULL,
    district_id BIGINT NULL,
    ward_id BIGINT NULL,
    hamlet_name VARCHAR(255) NULL,
    detail_address VARCHAR(500) NULL,
    full_address VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id),
    INDEX idx_address_type (address_type),
    UNIQUE KEY uk_staff_address_type (staff_id, address_type)
);

-- Bảng vị trí việc làm: Thông tin công việc hiện tại
CREATE TABLE IF NOT EXISTS staff_job_infos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL UNIQUE,
    main_subject_id BIGINT NULL,
    teaching_level_id BIGINT NULL,
    working_position_id BIGINT NULL,
    department_id BIGINT NULL,
    title_id BIGINT NULL,
    role_group_id BIGINT NULL,
    employment_type_id BIGINT NULL,
    recruitment_date DATE NULL,
    school_join_date DATE NULL,
    official_date DATE NULL,
    yearly_teaching_sessions INT NULL,
    daily_teaching_sessions DECIMAL(5,2) NULL,
    recruitment_agency VARCHAR(255) NULL,
    appointment_by VARCHAR(255) NULL,
    concurrent_task VARCHAR(255) NULL,
    is_ethnic_language_cert BOOLEAN DEFAULT FALSE,
    is_party_training BOOLEAN DEFAULT FALSE,
    is_retired BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id)
);

-- Bảng lịch sử công việc: Điều động, đổi chức danh...
CREATE TABLE IF NOT EXISTS staff_job_histories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NULL,
    unit_id BIGINT NULL,
    department_id BIGINT NULL,
    working_position_id BIGINT NULL,
    title_id BIGINT NULL,
    employment_type_id BIGINT NULL,
    decision_no VARCHAR(100) NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    FOREIGN KEY (unit_id) REFERENCES units(id),
    INDEX idx_staff_id (staff_id),
    INDEX idx_from_date (from_date)
);

-- Bảng lịch sử lương: Lương, phụ cấp
CREATE TABLE IF NOT EXISTS staff_salary_histories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    effective_date DATE NOT NULL,
    salary_rank_id BIGINT NULL,
    salary_code VARCHAR(50) NULL,
    salary_step INT NULL,
    salary_coefficient DECIMAL(5,2) NULL,
    vượt_khung_pct DECIMAL(5,2) NULL,
    seniority_pct DECIMAL(5,2) NULL,
    preferential_pct DECIMAL(5,2) NULL,
    responsibility_pct DECIMAL(5,2) NULL,
    position_allowance_pct DECIMAL(5,2) NULL,
    class_head_allowance_pct DECIMAL(5,2) NULL,
    homeroom_allowance_pct DECIMAL(5,2) NULL,
    hazardous_allowance_pct DECIMAL(5,2) NULL,
    region_allowance DECIMAL(10,2) NULL,
    long_term_allowance DECIMAL(10,2) NULL,
    attraction_allowance DECIMAL(10,2) NULL,
    is_on_salary_insurance BOOLEAN DEFAULT FALSE,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id),
    INDEX idx_effective_date (effective_date)
);

-- Bảng đào tạo: Trình độ chuyên môn
CREATE TABLE IF NOT EXISTS staff_educations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    education_type VARCHAR(30) NOT NULL,
    -- PROFESSIONAL, POLITICAL, IT, LANGUAGE, GENERAL
    level_id BIGINT NULL,
    major_id BIGINT NULL,
    school_name VARCHAR(255) NULL,
    training_form_id BIGINT NULL,
    graduation_year INT NULL,
    score VARCHAR(50) NULL,
    framework_level VARCHAR(50) NULL,
    is_highest BOOLEAN DEFAULT FALSE,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id),
    INDEX idx_education_type (education_type)
);

-- Bảng bồi dưỡng: Tập huấn, bồi dưỡng, thay sách...
CREATE TABLE IF NOT EXISTS staff_trainings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    training_type_id BIGINT NOT NULL,
    result VARCHAR(255) NULL,
    from_date DATE NULL,
    to_date DATE NULL,
    organizer VARCHAR(255) NULL,
    certificate_no VARCHAR(100) NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id),
    INDEX idx_from_date (from_date)
);

-- Bảng đảng/đoàn/công đoàn: Thông tin chính trị
CREATE TABLE IF NOT EXISTS staff_political_infos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL UNIQUE,
    marital_status_id BIGINT NULL,
    family_background VARCHAR(255) NULL,
    party_join_date DATE NULL,
    party_official_date DATE NULL,
    party_join_place VARCHAR(255) NULL,
    is_party_member BOOLEAN DEFAULT FALSE,
    is_union_member BOOLEAN DEFAULT FALSE,
    union_join_date DATE NULL,
    is_youth_union_member BOOLEAN DEFAULT FALSE,
    youth_union_join_date DATE NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id)
);

-- Bảng gia đình: Thân nhân, cha mẹ, vợ/chồng...
CREATE TABLE IF NOT EXISTS staff_family_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    relation_type VARCHAR(30) NOT NULL,
    -- FATHER, MOTHER, SPOUSE, CHILD, OTHER
    full_name VARCHAR(255) NOT NULL,
    birth_year INT NULL,
    occupation VARCHAR(255) NULL,
    workplace VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    INDEX idx_staff_id (staff_id),
    INDEX idx_relation_type (relation_type)
);

-- Bảng phân công giáo viên: Dạy lớp, dạy môn theo năm học
CREATE TABLE IF NOT EXISTS teacher_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    school_year_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    subject_id BIGINT NULL,
    is_homeroom BOOLEAN DEFAULT FALSE,
    department_id BIGINT NULL,
    teaching_load DECIMAL(5,2) NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    FOREIGN KEY (school_year_id) REFERENCES school_years(id),
    FOREIGN KEY (class_id) REFERENCES classrooms(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    INDEX idx_staff_id (staff_id),
    INDEX idx_school_year_id (school_year_id),
    INDEX idx_class_id (class_id),
    UNIQUE KEY uk_staff_schoolyear_class (staff_id, school_year_id, class_id)
);
