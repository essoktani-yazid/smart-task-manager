package com.smarttask.model;

import com.smarttask.model.CalendarEvent;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Adapter to make CalendarEvent compatible with PlannerItem interface
 * WITHOUT modifying the original CalendarEvent class
 */
public class EventPlannerAdapter implements PlannerItem {
    
    private final CalendarEvent event;
    
    public EventPlannerAdapter(CalendarEvent event) {
        this.event = event;
    }
    
    @Override
    public String getId() {
        return event.getId();
    }
    
    @Override
    public String getTitle() {
        return event.getTitle();
    }
    
    @Override
    public String getDescription() {
        return event.getDescription();
    }
    
    @Override
    public LocalDate getDate() {
        return event.getDate();
    }
    
    @Override
    public LocalTime getStartTime() {
        return event.getStartTime();
    }
    
    @Override
    public LocalTime getEndTime() {
        return event.getEndTime();
    }
    
    @Override
    public boolean isCompleted() {
        return event.isCompleted();
    }
    
    @Override
    public String getColor() {
        // Event uses its own color
        return event.getColor();
    }
    
    @Override
    public PlannerItemType getItemType() {
        return PlannerItemType.EVENT;
    }
    
    @Override
    public boolean occursOn(LocalDate date) {
        return event.occursOn(date);
    }
    
    @Override
    public int getPriorityLevel() {
        return switch (event.getPriority()) {
            case OPTIONAL -> 0;
            case STANDARD -> 1;
            case IMPORTANT -> 2;
            case URGENT -> 3;
        };
    }
    
    /**
     * Get the underlying CalendarEvent object
     */
    public CalendarEvent getEvent() {
        return event;
    }
    
    /**
     * Update event date/time (delegates to event)
     */
    public void setDate(LocalDate date) {
        event.setDate(date);
    }
    
    public void setStartTime(LocalTime startTime) {
        event.setStartTime(startTime);
    }
    
    public void setEndTime(LocalTime endTime) {
        event.setEndTime(endTime);
    }
}
