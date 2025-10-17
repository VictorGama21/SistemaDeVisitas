package com.inter.SistemaDeVisitas.entity;

public enum VisitStatus {
  PENDING("Pendente", "Pendentes", "text-bg-warning"),
  COMPLETED("Concluída", "Concluídas", "text-bg-success"),
  NO_SHOW("Não realizada", "Não compareceu", "text-bg-danger"),
  REOPENED("Reaberta", "Reabertas", "text-bg-info"),
  CANCELLED("Cancelada", "Canceladas", "text-bg-secondary");

  private final String label;
  private final String summaryLabel;
  private final String badgeClass;

  VisitStatus(String label, String summaryLabel, String badgeClass) {
    this.label = label;
    this.summaryLabel = summaryLabel;
    this.badgeClass = badgeClass;
  }

  public String getLabel() {
    return label;
  }

  public String getSummaryLabel() {
    return summaryLabel;
  }

  public String getBadgeClass() {
    return badgeClass;
  }
}
