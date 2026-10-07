ALTER TABLE payments
    ADD COLUMN refunded_by_user_id BIGINT NULL,
    ADD COLUMN refunded_at DATETIME(6) NULL,
    ADD CONSTRAINT fk_payment_refunded_by FOREIGN KEY (refunded_by_user_id) REFERENCES app_users(id);
