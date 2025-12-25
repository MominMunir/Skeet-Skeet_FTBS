-- Migration Script for SkeetSkeet Database
-- This script adds the reviews table and paymentMethod column to bookings
-- Run this script on your existing database to apply the changes

USE skeetskeet_db;

-- Add paymentMethod column to bookings table (if it doesn't exist)
-- Check if column exists first (MySQL 5.7+)
SET @dbname = DATABASE();
SET @tablename = "bookings";
SET @columnname = "paymentMethod";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = @tablename)
            AND (COLUMN_NAME = @columnname)
    ) > 0,
    "SELECT 'Column paymentMethod already exists in bookings table' AS result;",
    CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(50) NULL AFTER paymentStatus;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Create reviews table (if it doesn't exist)
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

-- Create indexes for reviews table (if they don't exist)
-- Note: MySQL will ignore IF NOT EXISTS for indexes, so we check first
SET @indexname1 = "idx_review_groundId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "reviews")
            AND (INDEX_NAME = @indexname1)
    ) > 0,
    "SELECT 'Index idx_review_groundId already exists' AS result;",
    "CREATE INDEX idx_review_groundId ON reviews(groundId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname2 = "idx_review_userId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "reviews")
            AND (INDEX_NAME = @indexname2)
    ) > 0,
    "SELECT 'Index idx_review_userId already exists' AS result;",
    "CREATE INDEX idx_review_userId ON reviews(userId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname3 = "idx_review_bookingId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "reviews")
            AND (INDEX_NAME = @indexname3)
    ) > 0,
    "SELECT 'Index idx_review_bookingId already exists' AS result;",
    "CREATE INDEX idx_review_bookingId ON reviews(bookingId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Create notifications table (if it doesn't exist)
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

-- Create indexes for notifications table (if they don't exist)
SET @indexname4 = "idx_notification_userId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "notifications")
            AND (INDEX_NAME = @indexname4)
    ) > 0,
    "SELECT 'Index idx_notification_userId already exists' AS result;",
    "CREATE INDEX idx_notification_userId ON notifications(userId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname5 = "idx_notification_isRead";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "notifications")
            AND (INDEX_NAME = @indexname5)
    ) > 0,
    "SELECT 'Index idx_notification_isRead already exists' AS result;",
    "CREATE INDEX idx_notification_isRead ON notifications(isRead);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname6 = "idx_notification_createdAt";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "notifications")
            AND (INDEX_NAME = @indexname6)
    ) > 0,
    "SELECT 'Index idx_notification_createdAt already exists' AS result;",
    "CREATE INDEX idx_notification_createdAt ON notifications(createdAt);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Create FCM tokens table (if it doesn't exist)
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    token TEXT NOT NULL,
    platform VARCHAR(50) DEFAULT 'android',
    createdAt BIGINT,
    lastUsed BIGINT,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_token (userId, token(255))
);

-- Create index for FCM tokens (if it doesn't exist)
SET @indexname7 = "idx_fcm_token_userId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "fcm_tokens")
            AND (INDEX_NAME = @indexname7)
    ) > 0,
    "SELECT 'Index idx_fcm_token_userId already exists' AS result;",
    "CREATE INDEX idx_fcm_token_userId ON fcm_tokens(userId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Create favorites table (if it doesn't exist)
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

-- Create indexes for favorites table (if they don't exist)
SET @indexname8 = "idx_favorite_userId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "favorites")
            AND (INDEX_NAME = @indexname8)
    ) > 0,
    "SELECT 'Index idx_favorite_userId already exists' AS result;",
    "CREATE INDEX idx_favorite_userId ON favorites(userId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SET @indexname9 = "idx_favorite_groundId";
SET @preparedStatement = (SELECT IF(
    (
        SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE
            (TABLE_SCHEMA = @dbname)
            AND (TABLE_NAME = "favorites")
            AND (INDEX_NAME = @indexname9)
    ) > 0,
    "SELECT 'Index idx_favorite_groundId already exists' AS result;",
    "CREATE INDEX idx_favorite_groundId ON favorites(groundId);"
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

SELECT 'Migration completed successfully!' AS result;
