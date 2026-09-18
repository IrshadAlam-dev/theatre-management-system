ALTER TABLE payments DROP CHECK ck_payment_status;

ALTER TABLE payments ADD CONSTRAINT ck_payment_status
    CHECK (status IN ('PENDING', 'CANCELLED', 'COMPLETED', 'FAILED', 'REFUNDED'));
