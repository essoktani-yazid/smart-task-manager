package com.smarttask.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Common interface for both Tasks and Events in the planner
 * Allows unified handling in the calendar views
 */
public interface PlannerItem {
    
    /**
     * Get unique identifier
     */
    String getId();
    
    /**
     * Get the title/name
     */
    String getTitle();
    
    /**
     * Get description
     */
    String getDescription();
    
    /**
     * Get the date this item occurs on
     */
    LocalDate getDate();
    
    /**
     * Get start time
     */
    LocalTime getStartTime();
    
    /**
     * Get end time
     */
    LocalTime getEndTime();
    
    /**
     * Check if completed
     */
    boolean isCompleted();
    
    /**
     * Get display color
     */
    String getColor();
    
    /**
     * Get item type (for visual differentiation)
     */
    PlannerItemType getItemType();
    
    /**
     * Check if this item occurs on a specific date
     */
    boolean occursOn(LocalDate date);
    
    /**
     * Get priority level (0-3 where 3 is highest)
     */
    int getPriorityLevel();
}
