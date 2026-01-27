package com.smarttask.model;

/**
 * Énumération représentant les niveaux de priorité d'une tâche.
 */
public enum Priority {
    LOW("Faible"),
    MEDIUM("Moyenne"),
    HIGH("Haute"),
    URGENT("Urgente");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}


