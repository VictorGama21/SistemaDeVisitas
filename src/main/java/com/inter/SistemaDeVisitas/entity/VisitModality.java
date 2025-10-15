package com.inter.SistemaDeVisitas.entity;

import java.text.Normalizer;

public enum VisitModality {
  PROMOTORIA_REPOSICAO("Promotoria - Reposição"),
  PROMOTORIA_ACAO_LOJA("Promotoria - Ação em Loja"),
  PROMOTORIA_DEGUSTACAO("Promotoria - Degustação");

  private final String label;

  VisitModality(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static VisitModality fromString(String value) {
    if (value == null || value.isBlank()) {
      return PROMOTORIA_REPOSICAO;
    }
    String trimmed = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("[^\\p{ASCII}]", "")
        .replace("-", " ")
        .replace("/", " ")
        .replaceAll("\\s+", " ")
        .trim()
        .toUpperCase();
    for (VisitModality modality : values()) {
      if (modality.name().equalsIgnoreCase(trimmed)) {
        return modality;
      }
      String normalizedLabel = Normalizer.normalize(modality.label, Normalizer.Form.NFD)
          .replaceAll("[^\\p{ASCII}]", "")
          .replace("-", " ")
          .replace("/", " ")
          .replaceAll("\\s+", " ")
          .trim()
          .toUpperCase();
      if (normalizedLabel.equals(trimmed)) {
        return modality;
      }
    }
    throw new IllegalArgumentException("Modalidade desconhecida: " + value);
  }
}
