-- ============================================
-- SMART TASK MANAGER - BASE DE DONNÉES COMPLÈTE
-- ============================================

-- 1. Créer la base de données
DROP DATABASE IF EXISTS smarttask_db;
CREATE DATABASE IF NOT EXISTS smarttask_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE smarttask_db;

-- ============================================
-- TABLES DE BASE (Déjà existantes)
-- ============================================

-- Table Users
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    preferences TEXT,
    
    -- Champs pour Google Calendar integration
    google_calendar_token TEXT,
    google_calendar_refresh_token TEXT,
    google_calendar_enabled BOOLEAN DEFAULT FALSE,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Table Tasks (pour MainTask et SubTask)
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    task_type ENUM('MAIN', 'SUB') NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    status ENUM('TODO', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED') DEFAULT 'TODO',
    due_date DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME,
    user_id VARCHAR(36) NOT NULL,
    parent_task_id VARCHAR(36),
    project_id VARCHAR(36),
    estimated_hours INT,
    actual_hours INT,
    recurrence_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'NONE') DEFAULT 'NONE',
    recurrence_interval INT DEFAULT 0,
    recurrence_end_date DATETIME,
    
    -- Champs pour AI categorization
    ai_category VARCHAR(50),
    ai_confidence_score FLOAT DEFAULT 0.0,
    ai_tags TEXT,
    
    -- Champs pour analytics
    difficulty_level ENUM('EASY', 'MEDIUM', 'HARD', 'VERY_HARD') DEFAULT 'MEDIUM',
    focus_required BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_task_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    INDEX idx_priority (priority),
    INDEX idx_ai_category (ai_category),
    INDEX idx_created_at (created_at),
    INDEX idx_completed_at (completed_at)
);

-- Table Task Tags (Many-to-Many)
CREATE TABLE task_tags (
    task_id VARCHAR(36),
    tag_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (task_id, tag_name),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    
    INDEX idx_tag_name (tag_name),
    INDEX idx_task_id (task_id)
);

-- Table Task Dependencies
CREATE TABLE task_dependencies (
    id VARCHAR(72) PRIMARY KEY, -- predecessor_id + "_" + successor_id
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
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Champs pour file attachments
    has_attachment BOOLEAN DEFAULT FALSE,
    attachment_count INT DEFAULT 0,
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- Table Comment Attachments
CREATE TABLE comment_attachments (
    id VARCHAR(36) PRIMARY KEY,
    comment_id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    
    INDEX idx_comment_id (comment_id),
    INDEX idx_file_type (file_type)
);

-- Table Time Tracking
CREATE TABLE time_tracking (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration_ms BIGINT,
    notes TEXT,
    
    -- Champs pour productivity analysis
    focus_score INT DEFAULT 0, -- 0-100
    interruptions INT DEFAULT 0,
    productivity_phase ENUM('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT') DEFAULT 'MORNING',
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time),
    INDEX idx_productivity_phase (productivity_phase)
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
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type ENUM('TASK_ASSIGNED', 'COMMENT_ADDED', 'DEADLINE_REMINDER', 'STATUS_CHANGE', 
              'DEPENDENCY_SATISFIED', 'PRODUCTIVITY_TIP', 'WEEKLY_SUMMARY') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    related_task_id VARCHAR(36),
    action_url VARCHAR(500),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (related_task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type)
);

-- ============================================
-- NOUVELLES TABLES POUR LES FONCTIONNALITÉS AVANCÉES
-- ============================================

-- Table Calendar Events
-- CREATE TABLE calendar_event (
--     id VARCHAR(36) PRIMARY KEY,
--     title VARCHAR(200) NOT NULL,
--     description TEXT,
--     start_time DATETIME NOT NULL,
--     end_time DATETIME NOT NULL,
--     location VARCHAR(200),
--     color VARCHAR(20) DEFAULT '#3788d8',
--     is_all_day BOOLEAN DEFAULT FALSE,
--     user_id VARCHAR(36) NOT NULL,
--     task_id VARCHAR(36),
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     google_event_id VARCHAR(255),
--     event_type ENUM('MEETING', 'TASK', 'REMINDER', 'PERSONAL', 'WORK') DEFAULT 'TASK',
--     recurrence_pattern TEXT,
--
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
--     FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
--
--     INDEX idx_user_id (user_id),
--     INDEX idx_start_time (start_time),
--     INDEX idx_end_time (end_time),
--     INDEX idx_task_id (task_id),
--     INDEX idx_event_type (event_type),
--     INDEX idx_is_all_day (is_all_day)
-- );

CREATE TABLE calendar_event (
                                id VARCHAR(36) PRIMARY KEY,
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
                                last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table AI Models (Pour stocker les modèles d'IA)
CREATE TABLE ai_models (
    id VARCHAR(36) PRIMARY KEY,
    model_type ENUM('CATEGORIZATION', 'TIME_PREDICTION', 'PRIORITY_PREDICTION', 'TEXT_PARSING') NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(20),
    model_data LONGBLOB,
    accuracy FLOAT DEFAULT 0.0,
    trained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    INDEX idx_model_type (model_type),
    INDEX idx_is_active (is_active)
);

-- Table Productivity Insights
CREATE TABLE productivity_insights (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    insight_type ENUM('WEEKLY_SUMMARY', 'PRODUCTIVITY_TIP', 'PERFORMANCE_TREND', 'TASK_SUGGESTION') NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_viewed BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_insight_type (insight_type),
    INDEX idx_created_at (created_at),
    INDEX idx_is_viewed (is_viewed)
);

-- Table Analytics Metrics
CREATE TABLE analytics_metrics (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    metric_date DATE NOT NULL,
    metric_type ENUM('TASKS_COMPLETED', 'TIME_TRACKED', 'PRODUCTIVITY_SCORE', 
                     'COMPLETION_RATE', 'OVERDUE_TASKS', 'FOCUS_TIME') NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    UNIQUE KEY unique_user_metric_date_type (user_id, metric_date, metric_type),
    INDEX idx_user_id (user_id),
    INDEX idx_metric_date (metric_date),
    INDEX idx_metric_type (metric_type)
);

-- Table NLP Patterns (Pour le parsing de langage naturel)
CREATE TABLE nlp_patterns (
    id VARCHAR(36) PRIMARY KEY,
    pattern_type ENUM('DATE', 'TIME', 'PRIORITY', 'CATEGORY', 'ACTION') NOT NULL,
    pattern_regex VARCHAR(500) NOT NULL,
    pattern_name VARCHAR(100),
    extraction_key VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    INDEX idx_pattern_type (pattern_type),
    INDEX idx_is_active (is_active)
);

-- Table Task Templates (Pour les tâches récurrentes/prédéfinies)
CREATE TABLE task_templates (
    id VARCHAR(36) PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    estimated_hours INT,
    category VARCHAR(50),
    tags TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_count INT DEFAULT 0,
    
    INDEX idx_template_name (template_name),
    INDEX idx_category (category),
    INDEX idx_usage_count (usage_count)
);

-- Table Projects
CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY,
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

-- Table User Productivity Settings
CREATE TABLE user_productivity_settings (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) UNIQUE NOT NULL,
    work_hours_start TIME DEFAULT '09:00:00',
    work_hours_end TIME DEFAULT '17:00:00',
    break_duration_minutes INT DEFAULT 15,
    break_frequency_minutes INT DEFAULT 90,
    daily_goal_tasks INT DEFAULT 5,
    weekly_goal_hours INT DEFAULT 40,
    focus_mode_enabled BOOLEAN DEFAULT TRUE,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    analytics_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id)
);

-- Table Teams
CREATE TABLE teams (
    id VARCHAR(36) PRIMARY KEY,
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
    team_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (team_id, project_id),
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

    INDEX idx_project_id (project_id)
);

-- ============================================
-- VUES POUR LES ANALYTIQUES
-- ============================================

-- Vue pour les statistiques quotidiennes
CREATE VIEW daily_stats AS
SELECT 
    u.id as user_id,
    u.username,
    DATE(t.created_at) as stat_date,
    COUNT(t.id) as total_tasks,
    SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) as pending_tasks,
    SUM(CASE WHEN t.due_date < NOW() AND t.status != 'COMPLETED' THEN 1 ELSE 0 END) as overdue_tasks,
    COALESCE(SUM(tt.duration_ms), 0) as total_time_ms
FROM users u
LEFT JOIN tasks t ON u.id = t.user_id
LEFT JOIN time_tracking tt ON t.id = tt.task_id AND DATE(tt.start_time) = DATE(t.created_at)
GROUP BY u.id, u.username, DATE(t.created_at);

-- Vue pour les métriques de productivité
CREATE VIEW productivity_metrics AS
SELECT 
    u.id as user_id,
    u.username,
    WEEK(t.created_at) as week_number,
    YEAR(t.created_at) as year,
    COUNT(DISTINCT t.id) as total_tasks,
    SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    ROUND((SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) / COUNT(DISTINCT t.id)) * 100, 2) as completion_rate,
    ROUND(COALESCE(AVG(tt.duration_ms / 3600000), 0), 2) as avg_task_hours,
    COUNT(DISTINCT CASE WHEN t.priority = 'URGENT' THEN t.id END) as urgent_tasks,
    COUNT(DISTINCT CASE WHEN t.priority = 'HIGH' THEN t.id END) as high_priority_tasks
FROM users u
LEFT JOIN tasks t ON u.id = t.user_id
LEFT JOIN time_tracking tt ON t.id = tt.task_id
GROUP BY u.id, u.username, WEEK(t.created_at), YEAR(t.created_at);

-- Vue pour les dépendances de tâches
CREATE VIEW task_dependencies_view AS
SELECT 
    td.id,
    t1.title as predecessor_title,
    t1.status as predecessor_status,
    t2.title as successor_title,
    t2.status as successor_status,
    td.dependency_type,
    CASE 
        WHEN td.dependency_type = 'FINISH_TO_START' AND t1.status = 'COMPLETED' THEN 'SATISFIED'
        WHEN td.dependency_type = 'START_TO_START' AND t1.status = 'IN_PROGRESS' THEN 'SATISFIED'
        ELSE 'PENDING'
    END as dependency_status
FROM task_dependencies td
JOIN tasks t1 ON td.predecessor_id = t1.id
JOIN tasks t2 ON td.successor_id = t2.id;

-- ============================================
-- DONNÉES DE TEST
-- ============================================

-- Insertion d'utilisateurs de test
INSERT INTO users (id, username, email, password_hash) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'admin', 'admin@smarttask.com', '$2a$12$YourHashedPasswordHere'),
('550e8400-e29b-41d4-a716-446655440001', 'john', 'john@smarttask.com', '$2a$12$YourHashedPasswordHere'),
('550e8400-e29b-41d4-a716-446655440002', 'jane', 'jane@smarttask.com', '$2a$12$YourHashedPasswordHere'),
('550e8400-e29b-41d4-a716-446655440003', 'alice', 'alice@smarttask.com', '$2a$12$YourHashedPasswordHere'),
('550e8400-e29b-41d4-a716-446655440004', 'bob', 'bob@smarttask.com', '$2a$12$YourHashedPasswordHere');

-- Insertion de paramètres de productivité
INSERT INTO user_productivity_settings (id, user_id, daily_goal_tasks, weekly_goal_hours) VALUES
(UUID(), '550e8400-e29b-41d4-a716-446655440000', 8, 35),
(UUID(), '550e8400-e29b-41d4-a716-446655440001', 5, 25),
(UUID(), '550e8400-e29b-41d4-a716-446655440002', 6, 30);

-- Insertion de projets
INSERT INTO projects (id, name, description, user_id, color) VALUES
('p001', 'Développement Application', 'Projet principal Smart Task Manager', '550e8400-e29b-41d4-a716-446655440000', '#FF6B6B'),
('p002', 'Documentation', 'Documentation technique et utilisateur', '550e8400-e29b-41d4-a716-446655440000', '#4ECDC4'),
('p003', 'Tests & QA', 'Tests qualité et validation', '550e8400-e29b-41d4-a716-446655440000', '#45B7D1');

-- Insertion de modèles d'IA
INSERT INTO ai_models (id, model_type, model_name, model_version, accuracy) VALUES
('ai001', 'CATEGORIZATION', 'TaskCategoryModel', '1.0.0', 0.85),
('ai002', 'TIME_PREDICTION', 'TimePredictionModel', '1.0.0', 0.78),
('ai003', 'PRIORITY_PREDICTION', 'PriorityModel', '1.0.0', 0.92);

-- Insertion de patterns NLP
INSERT INTO nlp_patterns (id, pattern_type, pattern_regex, pattern_name, extraction_key) VALUES
('nlp001', 'DATE', '\\b(tomorrow|today|next week|next month)\\b', 'Relative Date', 'date'),
('nlp002', 'TIME', '\\b(at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\b', 'Time Expression', 'time'),
('nlp003', 'PRIORITY', '\\b(urgent|high priority|low priority|asap)\\b', 'Priority Keywords', 'priority'),
('nlp004', 'CATEGORY', '\\b(meeting|call|email|report|shopping|doctor)\\b', 'Category Keywords', 'category');

-- Insertion de tâches de test
INSERT INTO tasks (id, title, description, task_type, priority, status, user_id, due_date, ai_category) VALUES
-- Tâches pour admin
('t001', 'Réunion équipe développement', 'Discussion architecture backend', 'MAIN', 'HIGH', 'TODO', 
 '550e8400-e29b-41d4-a716-446655440000', DATE_ADD(NOW(), INTERVAL 2 DAY), 'meeting'),
('t002', 'Implémenter système NLP', 'Parser langage naturel pour tâches', 'MAIN', 'URGENT', 'IN_PROGRESS',
 '550e8400-e29b-41d4-a716-446655440000', DATE_ADD(NOW(), INTERVAL 5 DAY), 'development'),
('t003', 'Tests unitaires DAO', 'Tests couverture 80% minimum', 'MAIN', 'MEDIUM', 'TODO',
 '550e8400-e29b-41d4-a716-446655440000', DATE_ADD(NOW(), INTERVAL 7 DAY), 'testing'),
('t004', 'Documentation API', 'Documenter endpoints REST', 'MAIN', 'LOW', 'TODO',
 '550e8400-e29b-41d4-a716-446655440000', DATE_ADD(NOW(), INTERVAL 10 DAY), 'documentation'),

-- Tâches pour john
('t005', 'Acheter fournitures bureau', 'Papier, stylos, café', 'MAIN', 'LOW', 'COMPLETED',
 '550e8400-e29b-41d4-a716-446655440001', DATE_SUB(NOW(), INTERVAL 1 DAY), 'shopping'),
('t006', 'Préparer rapport mensuel', 'Analyses et statistiques', 'MAIN', 'MEDIUM', 'IN_PROGRESS',
 '550e8400-e29b-41d4-a716-446655440001', NOW(), 'report'),

-- Tâches pour jane
('t007', 'Rendez-vous médecin', 'Contrôle annuel', 'MAIN', 'MEDIUM', 'TODO',
 '550e8400-e29b-41d4-a716-446655440002', DATE_ADD(NOW(), INTERVAL 3 DAY), 'health'),
('t008', 'Répondre aux emails', 'Priorité clients', 'MAIN', 'HIGH', 'TODO',
 '550e8400-e29b-41d4-a716-446655440002', NOW(), 'communication');

-- Sous-tâches
INSERT INTO tasks (id, title, description, task_type, priority, status, user_id, parent_task_id, due_date) VALUES
('st001', 'Design architecture NLP', 'Concevoir le modèle de données', 'SUB', 'HIGH', 'TODO',
 '550e8400-e29b-41d4-a716-446655440000', 't002', DATE_ADD(NOW(), INTERVAL 1 DAY)),
('st002', 'Implémenter parser date', 'Parser expressions temporelles', 'SUB', 'MEDIUM', 'IN_PROGRESS',
 '550e8400-e29b-41d4-a716-446655440000', 't002', DATE_ADD(NOW(), INTERVAL 2 DAY)),
('st003', 'Tests parser', 'Tests unitaires du parser', 'SUB', 'LOW', 'TODO',
 '550e8400-e29b-41d4-a716-446655440000', 't002', DATE_ADD(NOW(), INTERVAL 3 DAY));

-- Dépendances entre tâches
INSERT INTO task_dependencies (id, predecessor_id, successor_id, dependency_type) VALUES
('t002_t003', 't002', 't003', 'FINISH_TO_START'),
('st001_st002', 'st001', 'st002', 'START_TO_START'),
('st002_st003', 'st002', 'st003', 'FINISH_TO_START');

-- Tags pour tâches
INSERT INTO task_tags (task_id, tag_name) VALUES
('t001', 'meeting'),
('t001', 'team'),
('t002', 'ai'),
('t002', 'backend'),
('t002', 'development'),
('t003', 'testing'),
('t003', 'quality'),
('t004', 'documentation'),
('t005', 'shopping'),
('t006', 'report'),
('t007', 'health'),
('t008', 'email');

-- Événements calendrier
# INSERT INTO calendar_event (id, title, description, start_time, end_time, user_id, task_id, color) VALUES
# ('e001', 'Réunion équipe', 'Revue hebdomadaire',
#  DATE_ADD(NOW(), INTERVAL 1 DAY),
#  DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 1 HOUR),
#  '550e8400-e29b-41d4-a716-446655440000', 't001', '#FF6B6B'),
#
# ('e002', 'Développement NLP', 'Session focus',
#  DATE_ADD(NOW(), INTERVAL 2 DAY),
#  DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 3 HOUR),
#  '550e8400-e29b-41d4-a716-446655440000', 't002', '#4ECDC4'),
#
# ('e003', 'Rendez-vous médical', 'Contrôle annuel',
#  DATE_ADD(NOW(), INTERVAL 3 DAY),
#  DATE_ADD(DATE_ADD(NOW(), INTERVAL 3 DAY), INTERVAL 1 HOUR),
#  '550e8400-e29b-41d4-a716-446655440002', 't007', '#45B7D1');

-- Suivi du temps
INSERT INTO time_tracking (id, task_id, user_id, start_time, end_time, duration_ms, focus_score) VALUES
('tt001', 't002', '550e8400-e29b-41d4-a716-446655440000', 
 DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 3600000, 85),
('tt002', 't002', '550e8400-e29b-41d4-a716-446655440000',
 DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), 3600000, 90),
('tt003', 't006', '550e8400-e29b-41d4-a716-446655440001',
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 3600000, 75);

-- Commentaires
INSERT INTO comments (id, task_id, user_id, content) VALUES
('c001', 't002', '550e8400-e29b-41d4-a716-446655440000', 'J''ai commencé l''implémentation du parser de dates'),
('c002', 't002', '550e8400-e29b-41d4-a716-446655440001', 'Bonne idée d''ajouter le support des expressions relatives'),
('c003', 't006', '550e8400-e29b-41d4-a716-446655440001', 'Le rapport nécessite plus de données statistiques');

-- Notifications
INSERT INTO notifications (id, user_id, type, title, message, related_task_id) VALUES
('n001', '550e8400-e29b-41d4-a716-446655440000', 'TASK_ASSIGNED', 'Nouvelle tâche assignée', 
 'La tâche "Implémenter système NLP" vous a été assignée', 't002'),
('n002', '550e8400-e29b-41d4-a716-446655440000', 'DEADLINE_REMINDER', 'Rappel d''échéance',
 'La tâche "Réunion équipe développement" est due demain', 't001'),
('n003', '550e8400-e29b-41d4-a716-446655440002', 'PRODUCTIVITY_TIP', 'Conseil productivité',
 'Vous avez 2 tâches en attente, planifiez-les cette semaine', NULL);

-- Insights de productivité
INSERT INTO productivity_insights (id, user_id, insight_type, title, description) VALUES
('i001', '550e8400-e29b-41d4-a716-446655440000', 'WEEKLY_SUMMARY', 'Résumé hebdomadaire',
 'Vous avez complété 3 tâches sur 8 cette semaine. Productivité en hausse de 15%'),
('i002', '550e8400-e29b-41d4-a716-446655440000', 'PRODUCTIVITY_TIP', 'Optimisez votre temps',
 'Vos sessions du matin sont 25% plus productives. Planifiez les tâches difficiles avant midi');

-- Modèles de tâches
INSERT INTO task_templates (id, template_name, title, description, priority, category) VALUES
('tmp001', 'Réunion standard', 'Préparer réunion', 'Préparation ordre du jour et matériel', 'MEDIUM', 'meeting'),
('tmp002', 'Rapport mensuel', 'Rapport d''activité', 'Collecte données et rédaction', 'HIGH', 'report'),
('tmp003', 'Revue code', 'Revue de code', 'Vérification qualité et standards', 'MEDIUM', 'development');

-- ============================================
-- INDEX SUPPLÉMENTAIRES POUR PERFORMANCE
-- ============================================

-- Index pour les recherches fréquentes
CREATE INDEX idx_tasks_search ON tasks(title, description(100));
CREATE INDEX idx_tasks_user_status ON tasks(user_id, status);
CREATE INDEX idx_tasks_user_priority ON tasks(user_id, priority);
CREATE INDEX idx_time_tracking_date_range ON time_tracking(start_time, end_time);
-- CREATE INDEX idx_calendar_events_date_range ON calendar_event(start_time, end_time);
CREATE FULLTEXT INDEX idx_tasks_fulltext ON tasks(title, description);
CREATE FULLTEXT INDEX idx_comments_fulltext ON comments(content);
