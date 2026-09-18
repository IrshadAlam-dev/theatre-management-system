CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT ck_user_role CHECK (role IN ('ADMIN', 'CUSTOMER'))
);

CREATE TABLE theatres (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE screens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    theatre_id BIGINT NOT NULL,
    CONSTRAINT uk_screen_name UNIQUE (theatre_id, name),
    CONSTRAINT fk_screen_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id),
    CONSTRAINT ck_screen_capacity CHECK (capacity > 0)
);

CREATE TABLE movies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    genre VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    rating DECIMAL(3, 1),
    CONSTRAINT ck_movie_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_movie_rating CHECK (rating BETWEEN 0 AND 10)
);

CREATE TABLE movie_shows (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    show_date DATE NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    movie_id BIGINT NOT NULL,
    screen_id BIGINT NOT NULL,
    CONSTRAINT fk_show_movie FOREIGN KEY (movie_id) REFERENCES movies(id),
    CONSTRAINT fk_show_screen FOREIGN KEY (screen_id) REFERENCES screens(id),
    CONSTRAINT ck_show_times CHECK (end_time > start_time),
    INDEX ix_show_schedule (screen_id, show_date, start_time)
);

CREATE TABLE seats (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seat_number VARCHAR(10) NOT NULL,
    seat_row VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    screen_id BIGINT NOT NULL,
    CONSTRAINT uk_screen_seat UNIQUE (screen_id, seat_row, seat_number),
    CONSTRAINT fk_seat_screen FOREIGN KEY (screen_id) REFERENCES screens(id),
    CONSTRAINT ck_seat_price CHECK (price > 0),
    CONSTRAINT ck_seat_type CHECK (seat_type IN ('STANDARD', 'PREMIUM', 'RECLINER'))
);

CREATE TABLE bookings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(50) NOT NULL,
    booking_date DATETIME(6) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    CONSTRAINT uk_booking_reference UNIQUE (booking_reference),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_booking_show FOREIGN KEY (show_id) REFERENCES movie_shows(id),
    CONSTRAINT ck_booking_amount CHECK (total_amount > 0),
    CONSTRAINT ck_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

-- These rows represent current seat reservations, not payment history.
-- Cancellation will release these rows while retaining the booking and payment.
CREATE TABLE booking_seats (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT uk_show_seat UNIQUE (show_id, seat_id),
    CONSTRAINT fk_booking_seat_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_booking_seat_show FOREIGN KEY (show_id) REFERENCES movie_shows(id),
    CONSTRAINT fk_booking_seat_seat FOREIGN KEY (seat_id) REFERENCES seats(id),
    CONSTRAINT ck_booking_seat_price CHECK (price > 0)
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    method VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_payment_booking UNIQUE (booking_id),
    CONSTRAINT uk_payment_transaction UNIQUE (transaction_reference),
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT ck_payment_method CHECK (method IN ('CARD', 'UPI', 'CASH'))
);
