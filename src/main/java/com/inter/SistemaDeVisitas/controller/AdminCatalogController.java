package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Buyer;
import com.inter.SistemaDeVisitas.entity.Segment;
import com.inter.SistemaDeVisitas.entity.Supplier;
import com.inter.SistemaDeVisitas.repo.BuyerRepository;
import com.inter.SistemaDeVisitas.repo.SegmentRepository;
import com.inter.SistemaDeVisitas.repo.SupplierRepository;
import com.inter.SistemaDeVisitas.service.CsvImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
  public String index(Model model,
                      @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
                      @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
                      @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage,
                      @RequestParam(name = "buyerSearch", required = false) String buyerSearch,
                      @RequestParam(name = "supplierSearch", required = false) String supplierSearch,
                      @RequestParam(name = "segmentSearch", required = false) String segmentSearch,
                      HttpServletRequest request) {
    PageRequest buyerPageable = PageRequest.of(Math.max(0, buyerPage), 10, Sort.by("name").ascending());
    PageRequest supplierPageable = PageRequest.of(Math.max(0, supplierPage), 10, Sort.by("name").ascending());
    PageRequest segmentPageable = PageRequest.of(Math.max(0, segmentPage), 10, Sort.by("name").ascending());

    Page<Buyer> buyerResults = StringUtils.hasText(buyerSearch)
        ? buyers.findByNameContainingIgnoreCaseOrderByNameAsc(buyerSearch.trim(), buyerPageable)
        : buyers.findAll(buyerPageable);

    Page<Supplier> supplierResults = StringUtils.hasText(supplierSearch)
        ? suppliers.findByNameContainingIgnoreCaseOrderByNameAsc(supplierSearch.trim(), supplierPageable)
        : suppliers.findAll(supplierPageable);

    Page<Segment> segmentResults = StringUtils.hasText(segmentSearch)
        ? segments.findByNameContainingIgnoreCaseOrderByNameAsc(segmentSearch.trim(), segmentPageable)
        : segments.findAll(segmentPageable);

    model.addAttribute("buyerPage", buyerResults);
    model.addAttribute("supplierPage", supplierResults);
    model.addAttribute("segmentPage", segmentResults);
    model.addAttribute("buyerSearch", buyerSearch);
    model.addAttribute("supplierSearch", supplierSearch);
    model.addAttribute("segmentSearch", segmentSearch);
    model.addAttribute("catalogQuery", Optional.ofNullable(request.getQueryString()).orElse(""));
    return "admin/catalogos";
  }

  @PostMapping("/compradores")
  public String createBuyer(@RequestParam String name,
                            @RequestParam(name = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o comprador.");
      return redirectToCatalog(redirect);
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
    return redirectToCatalog(redirect);
  }

  @PostMapping("/fornecedores")
  public String createSupplier(@RequestParam String name,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o fornecedor.");
      return redirectToCatalog(redirect);
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
    return redirectToCatalog(redirect);
  }

  @PostMapping("/segmentos")
  public String createSegment(@RequestParam String name,
                              @RequestParam(name = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o segmento.");
      return redirectToCatalog(redirect);
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
    return redirectToCatalog(redirect);
  }

  @PostMapping("/compradores/{id}/status")
  public String toggleBuyer(@PathVariable Long id,
                            @RequestParam(name = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
    buyers.findById(id).ifPresentOrElse(buyer -> {
      buyer.setActive(!buyer.isActive());
      buyers.save(buyer);
      redirectAttributes.addFlashAttribute("successMessage", "Status do comprador atualizado.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/fornecedores/{id}/status")
  public String toggleSupplier(@PathVariable Long id,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    suppliers.findById(id).ifPresentOrElse(supplier -> {
      supplier.setActive(!supplier.isActive());
      suppliers.save(supplier);
      redirectAttributes.addFlashAttribute("successMessage", "Status do fornecedor atualizado.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/segmentos/{id}/status")
  public String toggleSegment(@PathVariable Long id,
                              @RequestParam(name = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
    segments.findById(id).ifPresentOrElse(segment -> {
      segment.setActive(!segment.isActive());
      segments.save(segment);
      redirectAttributes.addFlashAttribute("successMessage", "Status do segmento atualizado.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/compradores/importar")
  public String importBuyers(@RequestParam("file") MultipartFile file,
                             @RequestParam(name = "redirect", required = false) String redirect,
                             RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, redirect, buyers::findByNameIgnoreCase, name -> {
      Buyer buyer = new Buyer();
      buyer.setName(name);
      buyers.save(buyer);
    }, "compradores");
  }

  @PostMapping("/fornecedores/importar")
  public String importSuppliers(@RequestParam("file") MultipartFile file,
                                @RequestParam(name = "redirect", required = false) String redirect,
                                RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, redirect, suppliers::findByNameIgnoreCase, name -> {
      Supplier supplier = new Supplier();
      supplier.setName(name);
      suppliers.save(supplier);
    }, "fornecedores");
  }

  @PostMapping("/segmentos/importar")
  public String importSegments(@RequestParam("file") MultipartFile file,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, redirect, segments::findByNameIgnoreCase, name -> {
      Segment segment = new Segment();
      segment.setName(name);
      segments.save(segment);
    }, "segmentos");
  }

  @PostMapping("/compradores/{id}")
  public String updateBuyer(@PathVariable Long id,
                            @RequestParam String name,
                            @RequestParam(name = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome válido para o comprador.");
      return redirectToCatalog(redirect);
    }
    buyers.findById(id).ifPresentOrElse(buyer -> {
      buyer.setName(name.trim());
      buyers.save(buyer);
      redirectAttributes.addFlashAttribute("successMessage", "Comprador atualizado.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/fornecedores/{id}")
  public String updateSupplier(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome válido para o fornecedor.");
      return redirectToCatalog(redirect);
    }
    suppliers.findById(id).ifPresentOrElse(supplier -> {
      supplier.setName(name.trim());
      suppliers.save(supplier);
      redirectAttributes.addFlashAttribute("successMessage", "Fornecedor atualizado.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/segmentos/{id}")
  public String updateSegment(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam(name = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
    if (!StringUtils.hasText(name)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome válido para o segmento.");
      return redirectToCatalog(redirect);
    }
    segments.findById(id).ifPresentOrElse(segment -> {
      segment.setName(name.trim());
      segments.save(segment);
      redirectAttributes.addFlashAttribute("successMessage", "Segmento atualizado.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/compradores/{id}/excluir")
  public String deleteBuyer(@PathVariable Long id,
                            @RequestParam(name = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
    buyers.findById(id).ifPresentOrElse(buyer -> {
      buyers.delete(buyer);
      redirectAttributes.addFlashAttribute("successMessage", "Comprador removido.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/fornecedores/{id}/excluir")
  public String deleteSupplier(@PathVariable Long id,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    suppliers.findById(id).ifPresentOrElse(supplier -> {
      suppliers.delete(supplier);
      redirectAttributes.addFlashAttribute("successMessage", "Fornecedor removido.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado."));
    return redirectToCatalog(redirect);
  }

  @PostMapping("/segmentos/{id}/excluir")
  public String deleteSegment(@PathVariable Long id,
                              @RequestParam(name = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
    segments.findById(id).ifPresentOrElse(segment -> {
      segments.delete(segment);
      redirectAttributes.addFlashAttribute("successMessage", "Segmento removido.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado."));
    return redirectToCatalog(redirect);
  }

  private String importSimpleList(MultipartFile file,
                                  RedirectAttributes redirectAttributes,
                                  String redirect,
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
      return redirectToCatalog(redirect);
    }

    if (created > 0) {
      redirectAttributes.addFlashAttribute("successMessage", created + " " + entityLabel + " importado(s).");
    }
    if (!warnings.isEmpty()) {
      redirectAttributes.addFlashAttribute("warningMessages", warnings);
    }
    return redirectToCatalog(redirect);
  }

  private String redirectToCatalog(String redirect) {
    if (!StringUtils.hasText(redirect)) {
      return "redirect:/admin/catalogos";
    }
    return "redirect:/admin/catalogos?" + redirect;
  }
}
