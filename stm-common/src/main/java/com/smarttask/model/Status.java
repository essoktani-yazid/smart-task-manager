package com.smarttask.model;

/**
 * Énumération représentant les statuts possibles d'une tâche.
 */
public enum Status {
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    DONE("Terminée"),
    CANCELLED("Annulée");

    private final String label;

    Status(String label) {
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

