package com.inter.SistemaDeVisitas.service;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitModality;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class VisitExportService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  public byte[] export(List<Visit> visits,
                       LocalDate start,
                       LocalDate end,
                       Store store,
                       VisitFilterCriteria criteria) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Visitas");
      sheet.setDefaultColumnWidth(18);

      CellStyle headerStyle = workbook.createCellStyle();
      XSSFFont font = ((XSSFWorkbook) workbook).createFont();
      font.setBold(true);
      headerStyle.setFont(font);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      headerStyle.setFillForegroundColor((short) 0x35);
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);

      CellStyle textStyle = workbook.createCellStyle();
      textStyle.setWrapText(true);
      textStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
      textStyle.setBorderBottom(BorderStyle.THIN);
      textStyle.setBorderTop(BorderStyle.THIN);
      textStyle.setBorderLeft(BorderStyle.THIN);
      textStyle.setBorderRight(BorderStyle.THIN);

      CellStyle dateStyle = workbook.createCellStyle();
      dateStyle.cloneStyleFrom(textStyle);
      dateStyle.setAlignment(HorizontalAlignment.CENTER);

      int rowIndex = 0;
      var headerRow = sheet.createRow(rowIndex++);
      headerRow.setHeightInPoints(22);
      String period = start != null && end != null
          ? start.format(DATE_FORMAT) + " - " + end.format(DATE_FORMAT)
          : "Período completo";
      headerRow.createCell(0).setCellValue("Relatório de visitas");
      headerRow.getCell(0).setCellStyle(headerStyle);
      sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 11));

      var infoRow = sheet.createRow(rowIndex++);
      infoRow.createCell(0).setCellValue("Loja:");
      infoRow.createCell(1).setCellValue(store != null ? store.getName() : "Todas");
      infoRow.createCell(3).setCellValue("Período:");
      infoRow.createCell(4).setCellValue(period);

      var filterRow = sheet.createRow(rowIndex++);
      filterRow.createCell(0).setCellValue("Filtros aplicados:");
      filterRow.createCell(1).setCellValue(buildFilterSummary(criteria));
      sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 1, 11));

      rowIndex++;
      var columnsRow = sheet.createRow(rowIndex++);
      String[] columns = new String[] {
          "Data", "Lojas", "Status", "Modalidade", "Comprador", "Fornecedor", "Segmento",
          "Informações comerciais", "Observações", "Nota", "Última atualização", "Atualizado por"
      };
      for (int i = 0; i < columns.length; i++) {
        Cell cell = columnsRow.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
      }

      ZoneId zoneId = ZoneId.systemDefault();
      for (Visit visit : visits) {
        var row = sheet.createRow(rowIndex++);
        int col = 0;

        Cell dateCell = row.createCell(col++);
        if (visit.getScheduledDate() != null) {
          dateCell.setCellValue(visit.getScheduledDate().format(DATE_FORMAT));
        }
        dateCell.setCellStyle(dateStyle);

        Cell storesCell = row.createCell(col++);
        storesCell.setCellValue(visit.getStores().stream()
            .map(Store::getName)
            .sorted()
            .reduce((a, b) -> a + ", " + b)
            .orElse("-"));
        storesCell.setCellStyle(textStyle);

        Cell statusCell = row.createCell(col++);
        VisitStatus status = visit.getStatus();
        statusCell.setCellValue(status != null ? status.getLabel() : "-");
        statusCell.setCellStyle(textStyle);

        Cell modalityCell = row.createCell(col++);
        VisitModality modality = visit.getModality();
        modalityCell.setCellValue(modality != null ? modality.getLabel() : "-");
        modalityCell.setCellStyle(textStyle);

        Cell buyerCell = row.createCell(col++);
        buyerCell.setCellValue(visit.getBuyer() != null ? visit.getBuyer().getName() : "-");
        buyerCell.setCellStyle(textStyle);

        Cell supplierCell = row.createCell(col++);
        supplierCell.setCellValue(visit.getSupplier() != null ? visit.getSupplier().getName() : "-");
        supplierCell.setCellStyle(textStyle);

        Cell segmentCell = row.createCell(col++);
        segmentCell.setCellValue(visit.getSegment() != null ? visit.getSegment().getName() : "-");
        segmentCell.setCellStyle(textStyle);

        Cell commercialCell = row.createCell(col++);
        commercialCell.setCellValue(visit.getCommercialInfo() != null ? visit.getCommercialInfo() : "-");
        commercialCell.setCellStyle(textStyle);

        Cell commentCell = row.createCell(col++);
        commentCell.setCellValue(visit.getComment() != null ? visit.getComment() : "-");
        commentCell.setCellStyle(textStyle);

        Cell ratingCell = row.createCell(col++);
        if (visit.getRating() != null) {
          ratingCell.setCellValue(visit.getRating());
        } else {
          ratingCell.setCellValue("-");
        }
        ratingCell.setCellStyle(textStyle);

        Cell updatedAtCell = row.createCell(col++);
        Instant updatedAt = visit.getLastStatusUpdatedAt();
        if (updatedAt != null) {
          updatedAtCell.setCellValue(DATE_TIME_FORMAT.format(updatedAt.atZone(zoneId)));
        } else {
          updatedAtCell.setCellValue("-");
        }
        updatedAtCell.setCellStyle(textStyle);

        Cell updatedByCell = row.createCell(col++);
        updatedByCell.setCellValue(visit.getLastStatusUpdatedBy() != null ? visit.getLastStatusUpdatedBy().getName() : "-");
        updatedByCell.setCellStyle(textStyle);
      }

      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private String buildFilterSummary(VisitFilterCriteria criteria) {
    if (criteria == null) {
      return "Nenhum filtro adicional";
    }
    StringBuilder summary = new StringBuilder();
    if (criteria.hasStatusFilter()) {
      summary.append("Status: ");
      summary.append(criteria.getStatuses().stream().map(VisitStatus::getLabel).reduce((a, b) -> a + ", " + b).orElse("-"));
    }
    if (criteria.hasModalityFilter()) {
      appendSeparator(summary);
      summary.append("Modalidade: ");
      summary.append(criteria.getModalities().stream().map(VisitModality::getLabel).reduce((a, b) -> a + ", " + b).orElse("-"));
    }
    if (criteria.hasBuyerFilter()) {
      appendSeparator(summary);
      summary.append("Comprador filtrado");
    }
    if (criteria.hasSupplierFilter()) {
      appendSeparator(summary);
      summary.append("Fornecedor filtrado");
    }
    if (criteria.hasSegmentFilter()) {
      appendSeparator(summary);
      summary.append("Segmento filtrado");
    }
    if (criteria.hasDayOfWeekFilter()) {
      appendSeparator(summary);
      summary.append("Dia da semana: ");
      summary.append(criteria.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("pt", "BR")));
    }
    if (summary.length() == 0) {
      return "Nenhum filtro adicional";
    }
    return summary.toString();
  }

  private void appendSeparator(StringBuilder summary) {
    if (summary.length() > 0) {
      summary.append(" | ");
    }
  }
}
