ALTER TABLE payments
    ADD COLUMN confirmed_by_user_id BIGINT NULL,
    ADD COLUMN confirmed_at DATETIME(6) NULL,
    ADD CONSTRAINT fk_payment_confirmed_by FOREIGN KEY (confirmed_by_user_id) REFERENCES app_users(id);
