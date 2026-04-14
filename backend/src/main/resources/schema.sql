CREATE TABLE IF NOT EXISTS data_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100),
    CONSTRAINT uk_data_permissions_role_menu UNIQUE (role_id, menu_id),
    CONSTRAINT fk_data_permissions_roles FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_data_permissions_menus FOREIGN KEY (menu_id) REFERENCES menus(id)
);

CREATE TABLE IF NOT EXISTS data_permission_scopes (
    id BIGSERIAL PRIMARY KEY,
    data_permission_id BIGINT NOT NULL,
    scope_type VARCHAR(50) NOT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100),
    CONSTRAINT fk_dps_permission FOREIGN KEY (data_permission_id) REFERENCES data_permissions(id)
);

CREATE TABLE IF NOT EXISTS student_code_counters (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NULL,
    updated_by VARCHAR(100),
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_student_code_counters_unit_year UNIQUE (unit_id, year),
    CONSTRAINT fk_student_code_counters_units FOREIGN KEY (unit_id) REFERENCES units(id)
);
