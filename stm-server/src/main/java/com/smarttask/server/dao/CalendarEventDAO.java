package com.smarttask.server.dao;

import com.smarttask.model.CalendarEvent;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for CalendarEvent
 * Handles all database operations for calendar events
 * UPDATED: Now supports sharing, meeting links, and location
 */
public class CalendarEventDAO {

    private final DatabaseConnection dbConnection;

    public CalendarEventDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Create a new event in the database
     * UPDATED: Now includes visibility, sharing, meeting link, and location fields
     */
    public boolean createEvent(CalendarEvent event) {
        String sql = "INSERT INTO calendar_event (id, title, description, event_date, start_time, " +
                "end_time, priority, completed, event_type, periodic_type, days_in_week, " +
                "place_in_month, yearly_date, created_at, last_modified, " +
                // NEW FIELDS:
                "visibility, shared_with_user_ids, shared_with_emails, creator_user_id, " +
                "has_meeting_link, meeting_link, meeting_platform, meeting_password, location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setEventParameters(pstmt, event);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Event created: " + event.getTitle() +
                        " (Visibility: " + event.getVisibility() +
                        ", Has Meeting: " + event.hasMeetingLink() + ")");
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Error creating event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing event
     * UPDATED: Now includes all new fields
     */
    public boolean updateEvent(CalendarEvent event) {
        String sql = "UPDATE calendar_event SET title=?, description=?, event_date=?, start_time=?, " +
                "end_time=?, priority=?, completed=?, event_type=?, periodic_type=?, " +
                "days_in_week=?, place_in_month=?, yearly_date=?, last_modified=?, " +
                // NEW FIELDS:
                "visibility=?, shared_with_user_ids=?, shared_with_emails=?, " +
                "has_meeting_link=?, meeting_link=?, meeting_platform=?, meeting_password=?, " +
                "location=? WHERE id=?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, event.getTitle());
            pstmt.setString(2, event.getDescription());
            pstmt.setDate(3, event.getDate() != null ? Date.valueOf(event.getDate()) : null);
            pstmt.setTime(4, event.getStartTime() != null ? Time.valueOf(event.getStartTime()) : null);
            pstmt.setTime(5, event.getEndTime() != null ? Time.valueOf(event.getEndTime()) : null);
            pstmt.setString(6, event.getPriority().name());
            pstmt.setBoolean(7, event.isCompleted());
            pstmt.setString(8, event.getEventType().name());
            pstmt.setString(9, event.getPeriodicType() != null ? event.getPeriodicType().name() : null);
            pstmt.setString(10, event.getDaysInWeek());
            pstmt.setString(11, event.getPlaceInMonth() != null ? event.getPlaceInMonth().name() : null);
            pstmt.setDate(12, event.getYearlyDate() != null ? Date.valueOf(event.getYearlyDate()) : null);
            pstmt.setDate(13, Date.valueOf(LocalDate.now()));

            // NEW FIELDS:
            pstmt.setString(14, event.getVisibility() != null ? event.getVisibility().name() : "PUBLIC");
            pstmt.setString(15, listToString(event.getSharedWithUserIds()));
            pstmt.setString(16, listToString(event.getSharedWithEmails()));
            pstmt.setBoolean(17, event.hasMeetingLink());
            pstmt.setString(18, event.getMeetingLink());
            pstmt.setString(19, event.getMeetingPlatform());
            pstmt.setString(20, event.getMeetingPassword());
            pstmt.setString(21, event.getLocation());
            pstmt.setString(22, event.getId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Event updated: " + event.getTitle());
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Error updating event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete an event by ID
     */
    public boolean deleteEvent(String eventId) {
        String sql = "DELETE FROM calendar_event WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, eventId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get event by ID
     */
    public CalendarEvent getEventById(String eventId) {
        String sql = "SELECT * FROM calendar_event WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEvent(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting event by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get all events
     */
    public List<CalendarEvent> getAllEvents() {
        String sql = "SELECT * FROM calendar_event ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting all events: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * 🆕 NEW: Get events visible to a specific user
     * Returns: PUBLIC events + PRIVATE events created by user + SHARED events where user is in shared list
     */
    public List<CalendarEvent> getEventsForUser(String userId) {
        String sql = "SELECT * FROM calendar_event WHERE " +
                "visibility = 'PUBLIC' " +
                "OR (visibility = 'PRIVATE' AND creator_user_id = ?) " +
                "OR (visibility = 'SHARED' AND (creator_user_id = ? OR shared_with_user_ids LIKE ?)) " +
                "ORDER BY event_date, start_time";

        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            pstmt.setString(3, "%" + userId + "%");

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                CalendarEvent event = mapResultSetToEvent(rs);
                // Double-check if user is in shared list (SQL LIKE can have false positives)
                if (event.getVisibility() == CalendarEvent.EventVisibility.SHARED) {
                    if (event.getCreatorUserId().equals(userId) ||
                            event.getSharedWithUserIds().contains(userId)) {
                        events.add(event);
                    }
                } else {
                    events.add(event);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting events for user: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * 🆕 NEW: Get events created by a specific user
     */
    public List<CalendarEvent> getEventsByCreator(String userId) {
        String sql = "SELECT * FROM calendar_event WHERE creator_user_id = ? " +
                "ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting events by creator: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * 🆕 NEW: Get events shared with a specific user
     */
    public List<CalendarEvent> getEventsSharedWithUser(String userId) {
        String sql = "SELECT * FROM calendar_event WHERE " +
                "visibility = 'SHARED' AND shared_with_user_ids LIKE ? " +
                "ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + userId + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                CalendarEvent event = mapResultSetToEvent(rs);
                // Verify user is actually in the list
                if (event.getSharedWithUserIds().contains(userId)) {
                    events.add(event);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting shared events: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * 🆕 NEW: Get all events with meeting links
     */
    public List<CalendarEvent> getEventsWithMeetingLinks() {
        String sql = "SELECT * FROM calendar_event WHERE has_meeting_link = TRUE " +
                "ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting events with meeting links: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get events for a specific date
     */
    public List<CalendarEvent> getEventsByDate(LocalDate date) {
        String sql = "SELECT * FROM calendar_event WHERE event_date = ? ORDER BY start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting events by date: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get events in date range
     */
    public List<CalendarEvent> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM calendar_event WHERE event_date BETWEEN ? AND ? " +
                "ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting events by date range: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get events by priority
     */
    public List<CalendarEvent> getEventsByPriority(CalendarEvent.Priority priority) {
        String sql = "SELECT * FROM calendar_event WHERE priority = ? ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, priority.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting events by priority: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get completed events
     */
    public List<CalendarEvent> getCompletedEvents() {
        String sql = "SELECT * FROM calendar_event WHERE completed = TRUE ORDER BY event_date DESC";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting completed events: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get incomplete events
     */
    public List<CalendarEvent> getIncompleteEvents() {
        String sql = "SELECT * FROM calendar_event WHERE completed = FALSE ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting incomplete events: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get recurring events
     */
    public List<CalendarEvent> getRecurringEvents() {
        String sql = "SELECT * FROM calendar_event WHERE event_type = 'RECURRING_EVENT'";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting recurring events: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Search events by title
     */
    public List<CalendarEvent> searchEventsByTitle(String query) {
        String sql = "SELECT * FROM calendar_event WHERE title LIKE ? ORDER BY event_date, start_time";
        List<CalendarEvent> events = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching events: " + e.getMessage());
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Delete all completed events
     */
    public boolean deleteCompletedEvents() {
        String sql = "DELETE FROM calendar_event WHERE completed = TRUE";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println("🗑️ Deleted " + rowsAffected + " completed events");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting completed events: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete all events
     */
    public boolean deleteAllEvents() {
        String sql = "DELETE FROM calendar_event";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting all events: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get event count
     */
    public int getEventCount() {
        String sql = "SELECT COUNT(*) as count FROM calendar_event";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting event count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Set prepared statement parameters from CalendarEvent
     * UPDATED: Now includes new fields
     */
    private void setEventParameters(PreparedStatement pstmt, CalendarEvent event) throws SQLException {
        pstmt.setString(1, event.getId());
        pstmt.setString(2, event.getTitle());
        pstmt.setString(3, event.getDescription());
        pstmt.setDate(4, event.getDate() != null ? Date.valueOf(event.getDate()) : null);
        pstmt.setTime(5, event.getStartTime() != null ? Time.valueOf(event.getStartTime()) : null);
        pstmt.setTime(6, event.getEndTime() != null ? Time.valueOf(event.getEndTime()) : null);
        pstmt.setString(7, event.getPriority().name());
        pstmt.setBoolean(8, event.isCompleted());
        pstmt.setString(9, event.getEventType().name());
        pstmt.setString(10, event.getPeriodicType() != null ? event.getPeriodicType().name() : null);
        pstmt.setString(11, event.getDaysInWeek());
        pstmt.setString(12, event.getPlaceInMonth() != null ? event.getPlaceInMonth().name() : null);
        pstmt.setDate(13, event.getYearlyDate() != null ? Date.valueOf(event.getYearlyDate()) : null);
        pstmt.setDate(14, event.getCreatedAt() != null ? Date.valueOf(event.getCreatedAt()) : Date.valueOf(LocalDate.now()));
        pstmt.setDate(15, event.getLastModified() != null ? Date.valueOf(event.getLastModified()) : Date.valueOf(LocalDate.now()));

        // NEW FIELDS:
        pstmt.setString(16, event.getVisibility() != null ? event.getVisibility().name() : "PUBLIC");
        pstmt.setString(17, listToString(event.getSharedWithUserIds()));
        pstmt.setString(18, listToString(event.getSharedWithEmails()));
        pstmt.setString(19, event.getCreatorUserId());
        pstmt.setBoolean(20, event.hasMeetingLink());
        pstmt.setString(21, event.getMeetingLink());
        pstmt.setString(22, event.getMeetingPlatform());
        pstmt.setString(23, event.getMeetingPassword());
        pstmt.setString(24, event.getLocation());
    }

    /**
     * Map ResultSet to CalendarEvent object
     * UPDATED: Now includes new fields
     */
    private CalendarEvent mapResultSetToEvent(ResultSet rs) throws SQLException {
        CalendarEvent event = new CalendarEvent();

        event.setId(rs.getString("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));

        Date eventDate = rs.getDate("event_date");
        if (eventDate != null) {
            event.setDate(eventDate.toLocalDate());
        }

        Time startTime = rs.getTime("start_time");
        if (startTime != null) {
            event.setStartTime(startTime.toLocalTime());
        }

        Time endTime = rs.getTime("end_time");
        if (endTime != null) {
            event.setEndTime(endTime.toLocalTime());
        }

        String priority = rs.getString("priority");
        if (priority != null) {
            event.setPriority(CalendarEvent.Priority.valueOf(priority));
        }

        event.setCompleted(rs.getBoolean("completed"));

        String eventType = rs.getString("event_type");
        if (eventType != null) {
            event.setEventType(CalendarEvent.EventType.valueOf(eventType));
        }

        String periodicType = rs.getString("periodic_type");
        if (periodicType != null && !periodicType.isEmpty()) {
            event.setPeriodicType(CalendarEvent.PeriodicType.valueOf(periodicType));
        }

        event.setDaysInWeek(rs.getString("days_in_week"));

        String placeInMonth = rs.getString("place_in_month");
        if (placeInMonth != null && !placeInMonth.isEmpty()) {
            event.setPlaceInMonth(CalendarEvent.MonthPlace.valueOf(placeInMonth));
        }

        Date yearlyDate = rs.getDate("yearly_date");
        if (yearlyDate != null) {
            event.setYearlyDate(yearlyDate.toLocalDate());
        }

        Date createdAt = rs.getDate("created_at");
        if (createdAt != null) {
            event.setCreatedAt(createdAt.toLocalDate());
        }

        Date lastModified = rs.getDate("last_modified");
        if (lastModified != null) {
            event.setLastModified(lastModified.toLocalDate());
        }

        // NEW FIELDS:
        String visibility = rs.getString("visibility");
        if (visibility != null && !visibility.isEmpty()) {
            event.setVisibility(CalendarEvent.EventVisibility.valueOf(visibility));
        }

        event.setSharedWithUserIds(stringToList(rs.getString("shared_with_user_ids")));
        event.setSharedWithEmails(stringToList(rs.getString("shared_with_emails")));
        event.setCreatorUserId(rs.getString("creator_user_id"));
        event.setHasMeetingLink(rs.getBoolean("has_meeting_link"));
        event.setMeetingLink(rs.getString("meeting_link"));
        event.setMeetingPlatform(rs.getString("meeting_platform"));
        event.setMeetingPassword(rs.getString("meeting_password"));
        event.setLocation(rs.getString("location"));

        return event;
    }

    /**
     * Convert List<String> to comma-separated string for database storage
     */
    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }

    /**
     * Convert comma-separated string to List<String>
     */
    private List<String> stringToList(String str) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }
}