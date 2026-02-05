package com.smarttask.server.dao;

import com.smarttask.model.TaskDependency;
import com.smarttask.model.Task;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskDependencyDAO {

    public String save(TaskDependency dep) {
        String sql = "INSERT INTO task_dependencies (id, predecessor_id, successor_id, dependency_type, created_at) VALUES (?, ?, ?, ?, ?)";
        String id = dep.getId() != null ? dep.getId() : java.util.UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, dep.getPredecessor().getId());
            pstmt.setString(3, dep.getSuccessor().getId());
            pstmt.setString(4, dep.getType().name());
            pstmt.setTimestamp(5, new Timestamp(dep.getCreatedAt().getTime()));
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new SQLException("Failed to insert dependency");
            dep.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Optional<TaskDependency> findById(String id) {
        String sql = "SELECT * FROM task_dependencies WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<TaskDependency> findBySuccessorId(String successorId) {
        String sql = "SELECT * FROM task_dependencies WHERE successor_id = ?";
        List<TaskDependency> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, successorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<TaskDependency> findByPredecessorId(String predecessorId) {
        String sql = "SELECT * FROM task_dependencies WHERE predecessor_id = ?";
        List<TaskDependency> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, predecessorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM task_dependencies WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private TaskDependency map(ResultSet rs) throws SQLException {
        TaskDependency d = new TaskDependency();
        d.setId(rs.getString("id"));
        Task predecessor = new Task();
        predecessor.setId(rs.getString("predecessor_id"));
        d.setPredecessor(predecessor);
        Task successor = new Task();
        successor.setId(rs.getString("successor_id"));
        d.setSuccessor(successor);
        try {
            d.setType(TaskDependency.DependencyType.valueOf(rs.getString("dependency_type")));
        } catch (IllegalArgumentException e) {
            d.setType(TaskDependency.DependencyType.FINISH_TO_START);
        }
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) d.setCreatedAt(new java.util.Date(ct.getTime()));
        return d;
    }
}
