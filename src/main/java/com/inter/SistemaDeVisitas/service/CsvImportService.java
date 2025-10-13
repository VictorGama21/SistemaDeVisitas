package com.inter.SistemaDeVisitas.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

  public List<String[]> parse(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IOException("Arquivo CSV vazio ou inexistente");
    }

    List<String[]> rows = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String sanitized = line.strip();
        if (sanitized.isEmpty()) {
          continue;
        }
        rows.add(splitColumns(sanitized));
      }
    }
    return rows;
  }

  private String[] splitColumns(String line) {
    String delimiter = line.contains(";") ? ";" : ",";
    String[] raw = line.split(delimiter, -1);
    String[] trimmed = new String[raw.length];
    for (int i = 0; i < raw.length; i++) {
      trimmed[i] = raw[i].strip();
    }
    return trimmed;
  }
}
