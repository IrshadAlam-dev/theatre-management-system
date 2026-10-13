CREATE TABLE admin_activity (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    resource VARCHAR(40) NOT NULL,
    resource_id BIGINT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_admin_activity_admin FOREIGN KEY (admin_user_id) REFERENCES app_users(id),
    INDEX ix_admin_activity_occurred_at (occurred_at)
);
