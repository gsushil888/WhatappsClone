-- Create user_presence table manually if needed
CREATE TABLE IF NOT EXISTS user_presence (
    user_id BIGINT PRIMARY KEY,
    status ENUM('ONLINE', 'OFFLINE', 'AWAY', 'BUSY') DEFAULT 'OFFLINE',
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_online TIMESTAMP NULL,
    device_info VARCHAR(255),
    show_last_seen BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_presence_status ON user_presence(status);
CREATE INDEX IF NOT EXISTS idx_user_presence_last_seen ON user_presence(last_seen);

-- Check if table exists and has data
SELECT COUNT(*) as total_records FROM user_presence;
SELECT * FROM user_presence LIMIT 5;