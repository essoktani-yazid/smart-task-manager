package com.smarttask.model;

public enum Status {
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminée"),
    BLOCKED("Bloquée");

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