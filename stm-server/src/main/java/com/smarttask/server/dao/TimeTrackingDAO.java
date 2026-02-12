package com.smarttask.server.dao;

import com.smarttask.model.Task;
import com.smarttask.model.TimeTracking;
import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimeTrackingDAO {

    public List<TimeTracking> findByUserId(String userId) {
        String sql = "SELECT * FROM time_tracking WHERE user_id = ? ORDER BY start_time DESC";
        List<TimeTracking> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToTimeTracking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public List<TimeTracking> findByTaskId(String taskId) {
        String sql = "SELECT * FROM time_tracking WHERE task_id = ? ORDER BY start_time DESC";
        List<TimeTracking> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTimeTracking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public String save(TimeTracking tt) {
        String sql = "INSERT INTO time_tracking (id, task_id, user_id, start_time, end_time, duration_ms, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (tt.getId() == null) tt.setId(UUID.randomUUID().toString());
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tt.getId());
            pstmt.setString(2, tt.getTask() != null ? tt.getTask().getId() : null);
            pstmt.setString(3, tt.getUser() != null ? tt.getUser().getId() : null);
            
            // FIX: Convert java.util.Date to java.sql.Timestamp using constructor or getTime()
            pstmt.setTimestamp(4, tt.getStartTime() != null ? new Timestamp(tt.getStartTime().getTime()) : null);
            pstmt.setTimestamp(5, tt.getEndTime() != null ? new Timestamp(tt.getEndTime().getTime()) : null);
            
            if (tt.getDurationMs() != null) pstmt.setLong(6, tt.getDurationMs()); 
            else pstmt.setNull(6, Types.BIGINT);
            
            pstmt.setString(7, tt.getNotes());
            
            pstmt.executeUpdate();
            return tt.getId();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving time tracking", e);
        }
    }

    public void update(TimeTracking tt) {
        String sql = "UPDATE time_tracking SET end_time = ?, duration_ms = ?, notes = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // FIX: Convert java.util.Date to java.sql.Timestamp
            pstmt.setTimestamp(1, tt.getEndTime() != null ? new Timestamp(tt.getEndTime().getTime()) : null);
            
            if (tt.getDurationMs() != null) pstmt.setLong(2, tt.getDurationMs()); 
            else pstmt.setNull(2, Types.BIGINT);
            
            pstmt.setString(3, tt.getNotes());
            pstmt.setString(4, tt.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private TimeTracking mapResultSetToTimeTracking(ResultSet rs) throws SQLException {
        TimeTracking tt = new TimeTracking();
        tt.setId(rs.getString("id"));
        
        String tId = rs.getString("task_id");
        if (tId != null) {
            Task t = new Task(); t.setId(tId);
            tt.setTask(t);
        }
        
        String uId = rs.getString("user_id");
        if (uId != null) {
            User u = new User(); u.setId(uId);
            tt.setUser(u);
        }
        
        // FIX: Convert java.sql.Timestamp back to java.util.Date (Timestamp extends Date so direct assignment works or use explicit conversion)
        Timestamp start = rs.getTimestamp("start_time");
        if (start != null) tt.setStartTime(new java.util.Date(start.getTime()));

        Timestamp end = rs.getTimestamp("end_time");
        if (end != null) tt.setEndTime(new java.util.Date(end.getTime()));
        
        long dur = rs.getLong("duration_ms");
        if (!rs.wasNull()) tt.setDurationMs(dur);
        
        tt.setNotes(rs.getString("notes"));
        
        return tt;
    }
}