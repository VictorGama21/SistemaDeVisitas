package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Buyer;
import com.inter.SistemaDeVisitas.entity.Segment;
import com.inter.SistemaDeVisitas.entity.Supplier;
import com.inter.SistemaDeVisitas.repo.BuyerRepository;
import com.inter.SistemaDeVisitas.repo.SegmentRepository;
import com.inter.SistemaDeVisitas.repo.SupplierRepository;
import com.inter.SistemaDeVisitas.service.CsvImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Controller
@RequestMapping("/admin/catalogos")
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
public class AdminCatalogController {

  private final BuyerRepository buyers;
  private final SupplierRepository suppliers;
  private final SegmentRepository segments;
  private final CsvImportService csvImportService;

  public AdminCatalogController(BuyerRepository buyers,
                                SupplierRepository suppliers,
                                SegmentRepository segments,
                                CsvImportService csvImportService) {
    this.buyers = buyers;
    this.suppliers = suppliers;
    this.segments = segments;
    this.csvImportService = csvImportService;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("buyers", buyers.findAllByOrderByNameAsc());
    model.addAttribute("suppliers", suppliers.findAllByOrderByNameAsc());
    model.addAttribute("segments", segments.findAllByOrderByNameAsc());
    return "admin/catalogos";
  }

  @PostMapping("/compradores")
  public String createBuyer(@RequestParam String name, RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o comprador.");
      return "redirect:/admin/catalogos";
    }
    buyers.findByNameIgnoreCase(name.trim()).ifPresentOrElse(existing -> {
      existing.setActive(true);
      buyers.save(existing);
    }, () -> {
      Buyer buyer = new Buyer();
      buyer.setName(name.trim());
      buyers.save(buyer);
    });
    redirectAttributes.addFlashAttribute("successMessage", "Comprador salvo com sucesso.");
    return "redirect:/admin/catalogos";
  }

  @PostMapping("/fornecedores")
  public String createSupplier(@RequestParam String name, RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o fornecedor.");
      return "redirect:/admin/catalogos";
    }
    suppliers.findByNameIgnoreCase(name.trim()).ifPresentOrElse(existing -> {
      existing.setActive(true);
      suppliers.save(existing);
    }, () -> {
      Supplier supplier = new Supplier();
      supplier.setName(name.trim());
      suppliers.save(supplier);
    });
    redirectAttributes.addFlashAttribute("successMessage", "Fornecedor salvo com sucesso.");
    return "redirect:/admin/catalogos";
  }

  @PostMapping("/segmentos")
  public String createSegment(@RequestParam String name, RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o segmento.");
      return "redirect:/admin/catalogos";
    }
    segments.findByNameIgnoreCase(name.trim()).ifPresentOrElse(existing -> {
      existing.setActive(true);
      segments.save(existing);
    }, () -> {
      Segment segment = new Segment();
      segment.setName(name.trim());
      segments.save(segment);
    });
    redirectAttributes.addFlashAttribute("successMessage", "Segmento salvo com sucesso.");
    return "redirect:/admin/catalogos";
  }

  @PostMapping("/compradores/{id}/status")
  public String toggleBuyer(@PathVariable Long id) {
    buyers.findById(id).ifPresent(buyer -> {
      buyer.setActive(!buyer.isActive());
      buyers.save(buyer);
    });
    return "redirect:/admin/catalogos";
  }

  @PostMapping("/fornecedores/{id}/status")
  public String toggleSupplier(@PathVariable Long id) {
    suppliers.findById(id).ifPresent(supplier -> {
      supplier.setActive(!supplier.isActive());
      suppliers.save(supplier);
    });
    return "redirect:/admin/catalogos";
  }

  @PostMapping("/segmentos/{id}/status")
  public String toggleSegment(@PathVariable Long id) {
    segments.findById(id).ifPresent(segment -> {
      segment.setActive(!segment.isActive());
      segments.save(segment);
    });
    return "redirect:/admin/catalogos";
  }

  @PostMapping("/compradores/importar")
  public String importBuyers(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, buyers::findByNameIgnoreCase, name -> {
      Buyer buyer = new Buyer();
      buyer.setName(name);
      buyers.save(buyer);
    }, "compradores");
  }

  @PostMapping("/fornecedores/importar")
  public String importSuppliers(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, suppliers::findByNameIgnoreCase, name -> {
      Supplier supplier = new Supplier();
      supplier.setName(name);
      suppliers.save(supplier);
    }, "fornecedores");
  }

  @PostMapping("/segmentos/importar")
  public String importSegments(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, segments::findByNameIgnoreCase, name -> {
      Segment segment = new Segment();
      segment.setName(name);
      segments.save(segment);
    }, "segmentos");
  }

  private String importSimpleList(MultipartFile file,
                                  RedirectAttributes redirectAttributes,
                                  Function<String, Optional<?>> finder,
                                  Consumer<String> creator,
                                  String entityLabel) {
    int created = 0;
    List<String> warnings = new ArrayList<>();
    try {
      for (String[] row : csvImportService.parse(file)) {
        if (row.length == 0 || !StringUtils.hasText(row[0]) || row[0].toLowerCase().contains("nome")) {
          continue;
        }
        String name = row[0].trim();
        if (finder.apply(name).isPresent()) {
          warnings.add("Já existia: " + name);
        } else {
          creator.accept(name);
          created++;
        }
      }
    } catch (IOException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Erro ao importar " + entityLabel + ": " + e.getMessage());
      return "redirect:/admin/catalogos";
    }

    if (created > 0) {
      redirectAttributes.addFlashAttribute("successMessage", created + " " + entityLabel + " importado(s).");
    }
    if (!warnings.isEmpty()) {
      redirectAttributes.addFlashAttribute("warningMessages", warnings);
    }
    return "redirect:/admin/catalogos";
  }
}
