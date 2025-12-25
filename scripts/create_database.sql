-- Create Database
CREATE DATABASE IF NOT EXISTS skeetskeet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE skeetskeet_db;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    fullName VARCHAR(255) NOT NULL,
    role ENUM('PLAYER', 'GROUNDKEEPER', 'ADMIN') DEFAULT 'PLAYER',
    phoneNumber VARCHAR(20),
    profileImageUrl TEXT,
    createdAt BIGINT,
    synced BOOLEAN DEFAULT 1
);

-- Grounds Table
CREATE TABLE IF NOT EXISTS grounds (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    priceText VARCHAR(50),
    rating DECIMAL(3,2) DEFAULT 0.0,
    ratingText VARCHAR(10),
    imageUrl TEXT,
    imagePath VARCHAR(255),
    hasFloodlights BOOLEAN DEFAULT 1,
    hasParking BOOLEAN DEFAULT 1,
    description TEXT,
    available BOOLEAN DEFAULT 1,
    synced BOOLEAN DEFAULT 1
);

-- Bookings Table
CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(255) PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    groundId VARCHAR(255) NOT NULL,
    groundName VARCHAR(255) NOT NULL,
    date VARCHAR(50) NOT NULL,
    time VARCHAR(50) NOT NULL,
    duration INT DEFAULT 1,
    totalPrice DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING',
    paymentId VARCHAR(255),
    paymentStatus ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    createdAt BIGINT,
    updatedAt BIGINT,
    synced BOOLEAN DEFAULT 1,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (groundId) REFERENCES grounds(id) ON DELETE CASCADE
);

-- Reviews Table
CREATE TABLE IF NOT EXISTS reviews (
    id VARCHAR(255) PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    groundId VARCHAR(255) NOT NULL,
    bookingId VARCHAR(255),
    rating DECIMAL(3,2) NOT NULL,
    reviewText TEXT,
    createdAt BIGINT,
    synced BOOLEAN DEFAULT 1,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (groundId) REFERENCES grounds(id) ON DELETE CASCADE,
    FOREIGN KEY (bookingId) REFERENCES bookings(id) ON DELETE SET NULL
);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('BOOKING', 'PAYMENT', 'REMINDER', 'SYSTEM') DEFAULT 'SYSTEM',
    relatedId VARCHAR(255),
    isRead BOOLEAN DEFAULT 0,
    createdAt BIGINT,
    synced BOOLEAN DEFAULT 1,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
);

-- Favorites Table
CREATE TABLE IF NOT EXISTS favorites (
    id VARCHAR(255) PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    groundId VARCHAR(255) NOT NULL,
    createdAt BIGINT,
    synced BOOLEAN DEFAULT 1,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (groundId) REFERENCES grounds(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_ground (userId, groundId)
);

-- Create indexes for better performance
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_booking_userId ON bookings(userId);
CREATE INDEX idx_booking_groundId ON bookings(groundId);
CREATE INDEX idx_booking_date ON bookings(date);
CREATE INDEX idx_review_groundId ON reviews(groundId);
CREATE INDEX idx_review_userId ON reviews(userId);
CREATE INDEX idx_review_bookingId ON reviews(bookingId);
CREATE INDEX idx_notification_userId ON notifications(userId);
CREATE INDEX idx_notification_isRead ON notifications(isRead);
CREATE INDEX idx_notification_createdAt ON notifications(createdAt);
CREATE INDEX idx_favorite_userId ON favorites(userId);
CREATE INDEX idx_favorite_groundId ON favorites(groundId);
