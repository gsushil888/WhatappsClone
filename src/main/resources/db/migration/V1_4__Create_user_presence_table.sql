CREATE TABLE user_presence (
    user_id BIGINT PRIMARY KEY,
    status ENUM('ONLINE', 'OFFLINE', 'AWAY', 'BUSY') DEFAULT 'OFFLINE',
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_online TIMESTAMP NULL,
    device_info VARCHAR(255),
    show_last_seen BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_presence_status ON user_presence(status);
CREATE INDEX idx_user_presence_last_seen ON user_presence(last_seen);