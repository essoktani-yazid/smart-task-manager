-- Script pour créer les tables Teams si elles n'existent pas déjà
USE smarttask_db;

-- Table Teams
CREATE TABLE IF NOT EXISTS teams (
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
CREATE TABLE IF NOT EXISTS team_members (
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
CREATE TABLE IF NOT EXISTS team_projects (
    team_id VARCHAR(36) NOT NULL DEFAULT (UUID()),
    project_id VARCHAR(36) NOT NULL DEFAULT (UUID()),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (team_id, project_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

    INDEX idx_project_id (project_id)
);
