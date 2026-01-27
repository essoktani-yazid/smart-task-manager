package com.smarttask.model;

import java.util.Date;
import java.util.UUID;

/**
 * Plain model representing a dependency between two tasks (no JPA annotations).
 */
public class TaskDependency {

    private String id;

    // Task that must be finished first
    private Task predecessor;

    // Task that depends on the predecessor
    private Task successor;

    private DependencyType type = DependencyType.FINISH_TO_START;

    private Date createdAt;

    // Types de dépendances
    public enum DependencyType {
        FINISH_TO_START,    // Finir A avant de commencer B
        START_TO_START,     // Commencer A et B en même temps
        FINISH_TO_FINISH,   // Finir A et B en même temps
        START_TO_FINISH     // Commencer A avant de finir B
    }

    // Constructeurs
    public TaskDependency() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
    }

    public TaskDependency(Task predecessor, Task successor, DependencyType type) {
        this();
        this.predecessor = predecessor;
        this.successor = successor;
        this.type = type;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Task getPredecessor() { return predecessor; }
    public void setPredecessor(Task predecessor) { this.predecessor = predecessor; }

    public Task getSuccessor() { return successor; }
    public void setSuccessor(Task successor) { this.successor = successor; }

    public DependencyType getType() { return type; }
    public void setType(DependencyType type) { this.type = type; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    /**
     * Vérifie si la dépendance est satisfaite
     */
    public boolean isSatisfied() {
        switch (type) {
            case FINISH_TO_START:
                return predecessor.getStatus() == Status.COMPLETED;
            case START_TO_START:
                return predecessor.getStatus() == Status.IN_PROGRESS;
            case FINISH_TO_FINISH:
                return predecessor.getStatus() == Status.COMPLETED &&
                        successor.getStatus() == Status.COMPLETED;
            case START_TO_FINISH:
                return predecessor.getStatus() == Status.IN_PROGRESS;
            default:
                return false;
        }
    }
}