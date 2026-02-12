-- 1. Créer la base de données
DROP DATABASE IF EXISTS smarttask_db;
CREATE DATABASE IF NOT EXISTS smarttask_db ;

USE smarttask_db;

-- Table Users
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    google_calendar_token TEXT,
    google_calendar_refresh_token TEXT,
    google_calendar_enabled BOOLEAN DEFAULT FALSE,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Table Tasks
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    status ENUM('TODO', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED') DEFAULT 'TODO',
    due_date DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME,
    user_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36),
    recurrence_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'NONE') DEFAULT 'NONE',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- ⚡ INDEXES POUR PERFORMANCE
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_due_date (due_date),
    INDEX idx_created_at (created_at),
    INDEX idx_project_id (project_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_due_date (user_id, due_date)
);

-- Table sub_tasks
CREATE TABLE sub_tasks (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    task_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    status ENUM('TODO', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED') DEFAULT 'TODO',
    due_date DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME,
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
);


-- Table Task Tags (Many-to-Many)
CREATE TABLE task_tags (
    task_id VARCHAR(36) DEFAULT (UUID()),
    tag_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (task_id, tag_name),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    
    INDEX idx_tag_name (tag_name),
    INDEX idx_task_id (task_id)
);

-- Table Task Dependencies
CREATE TABLE task_dependencies (
    id VARCHAR(72) PRIMARY KEY DEFAULT (UUID()), 
    predecessor_id VARCHAR(36) NOT NULL,
    successor_id VARCHAR(36) NOT NULL,
    dependency_type ENUM('FINISH_TO_START', 'START_TO_START', 'FINISH_TO_FINISH', 'START_TO_FINISH') DEFAULT 'FINISH_TO_START',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (predecessor_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (successor_id) REFERENCES tasks(id) ON DELETE CASCADE,
    
    UNIQUE KEY unique_dependency (predecessor_id, successor_id),
    INDEX idx_predecessor (predecessor_id),
    INDEX idx_successor (successor_id),
    INDEX idx_dependency_type (dependency_type)
);

-- Table Comments
CREATE TABLE comments (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- Table Comment Attachments
CREATE TABLE comment_attachments (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    comment_id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE
    
);

-- Table Shared Tasks
CREATE TABLE shared_tasks (
    task_id VARCHAR(36),
    user_id VARCHAR(36),
    permission_level ENUM('READ', 'WRITE', 'ADMIN') DEFAULT 'READ',
    shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    can_edit BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    can_share BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_task_id (task_id),
    INDEX idx_permission_level (permission_level)
);

-- Table Notifications
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id VARCHAR(36) NOT NULL,
    type ENUM('TASK_ASSIGNED', 'COMMENT_ADDED', 'DEADLINE_REMINDER', 'STATUS_CHANGE', 
              'DEPENDENCY_SATISFIED', 'PRODUCTIVITY_TIP', 'WEEKLY_SUMMARY') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    related_task_id VARCHAR(36),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (related_task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type)
);

CREATE TABLE calendar_event (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    priority VARCHAR(20) DEFAULT 'STANDARD',
    completed BOOLEAN DEFAULT FALSE,
    event_type VARCHAR(30) DEFAULT 'ONE_TIME_EVENT',
    periodic_type VARCHAR(20),
    days_in_week VARCHAR(50),
    place_in_month VARCHAR(20),
    yearly_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    visibility VARCHAR(20) DEFAULT 'PUBLIC',
    shared_with_user_ids TEXT,
    shared_with_emails TEXT,
    creator_user_id VARCHAR(36),
    has_meeting_link BOOLEAN DEFAULT FALSE,
    meeting_link TEXT,
    meeting_platform VARCHAR(50),
    meeting_password VARCHAR(255),
    location VARCHAR(500)
);

-- Table Projects
CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    user_id VARCHAR(36) NOT NULL,
    color VARCHAR(20) DEFAULT '#3788d8',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_name (name),
    INDEX idx_is_active (is_active)
);

-- Table Teams
CREATE TABLE teams (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(20) DEFAULT '#3788d8',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    owner_id VARCHAR(36) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,

    UNIQUE KEY unique_team_name (name),
    INDEX idx_owner_id (owner_id),
    INDEX idx_is_active (is_active)
);

-- Table Team Members (Many-to-Many: users ↔ teams)
CREATE TABLE team_members (
    team_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role ENUM('MEMBER', 'ADMIN', 'OWNER') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (team_id, user_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_user_id (user_id),
    INDEX idx_role (role)
);

-- Table Team Projects
CREATE TABLE team_projects (
    team_id VARCHAR(36) NOT NULL DEFAULT (UUID()),
    project_id VARCHAR(36) NOT NULL DEFAULT (UUID()),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (team_id, project_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

    INDEX idx_project_id (project_id)
);