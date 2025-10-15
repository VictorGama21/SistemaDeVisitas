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
  public class AdminCatalogController {
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
    Optional<Buyer> existingBuyer = buyers.findByNameIgnoreCase(name.trim());
    if (existingBuyer.isPresent()) {
      Buyer buyer = existingBuyer.get();
      buyer.setActive(true);
      buyers.save(buyer);
    } else {
      Buyer buyer = new Buyer();
      buyer.setName(name.trim());
      buyers.save(buyer);
    }
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
    Optional<Supplier> existingSupplier = suppliers.findByNameIgnoreCase(name.trim());
    if (existingSupplier.isPresent()) {
      Supplier supplier = existingSupplier.get();
      supplier.setActive(true);
      suppliers.save(supplier);
    } else {
      Supplier supplier = new Supplier();
      supplier.setName(name.trim());
      suppliers.save(supplier);
    }
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
    Optional<Segment> existingSegment = segments.findByNameIgnoreCase(name.trim());
    if (existingSegment.isPresent()) {
      Segment segment = existingSegment.get();
      segment.setActive(true);
      segments.save(segment);
    } else {
      Segment segment = new Segment();
      segment.setName(name.trim());
      segments.save(segment);
    }
    redirectAttributes.addFlashAttribute("successMessage", "Segmento salvo com sucesso.");
    return redirectToCatalog(redirect);
  }

  @PostMapping("/compradores/{id}/status")
  public String toggleBuyer(@PathVariable Long id,
                            @RequestParam(name = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
    Optional<Buyer> buyerOptional = buyers.findById(id);
    if (buyerOptional.isPresent()) {
      Buyer buyer = buyerOptional.get();
      buyer.setActive(!buyer.isActive());
      buyers.save(buyer);
      redirectAttributes.addFlashAttribute("successMessage", "Status do comprador atualizado.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  @PostMapping("/fornecedores/{id}/status")
  public String toggleSupplier(@PathVariable Long id,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    Optional<Supplier> supplierOptional = suppliers.findById(id);
    if (supplierOptional.isPresent()) {
      Supplier supplier = supplierOptional.get();
      supplier.setActive(!supplier.isActive());
      suppliers.save(supplier);
      redirectAttributes.addFlashAttribute("successMessage", "Status do fornecedor atualizado.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  @PostMapping("/segmentos/{id}/status")
  public String toggleSegment(@PathVariable Long id,
                              @RequestParam(name = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
    Optional<Segment> segmentOptional = segments.findById(id);
    if (segmentOptional.isPresent()) {
      Segment segment = segmentOptional.get();
      segment.setActive(!segment.isActive());
      segments.save(segment);
      redirectAttributes.addFlashAttribute("successMessage", "Status do segmento atualizado.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  @PostMapping("/compradores/importar")
  public String importBuyers(@RequestParam("file") MultipartFile file,
                             @RequestParam(name = "redirect", required = false) String redirect,
                             RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, redirect,
        new EntityFinder() {
          @Override
          public Optional<?> findByName(String value) {
            return buyers.findByNameIgnoreCase(value);
          }
        },
        new EntityCreator() {
          @Override
          public void create(String value) {
            Buyer buyer = new Buyer();
            buyer.setName(value);
            buyers.save(buyer);
          }
        },
        "compradores");
  }

  @PostMapping("/fornecedores/importar")
  public String importSuppliers(@RequestParam("file") MultipartFile file,
                                @RequestParam(name = "redirect", required = false) String redirect,
                                RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, redirect,
        new EntityFinder() {
          @Override
          public Optional<?> findByName(String value) {
            return suppliers.findByNameIgnoreCase(value);
          }
        },
        new EntityCreator() {
          @Override
          public void create(String value) {
            Supplier supplier = new Supplier();
            supplier.setName(value);
            suppliers.save(supplier);
          }
        },
        "fornecedores");
  }

  @PostMapping("/segmentos/importar")
  public String importSegments(@RequestParam("file") MultipartFile file,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    return importSimpleList(file, redirectAttributes, redirect,
        new EntityFinder() {
          @Override
          public Optional<?> findByName(String value) {
            return segments.findByNameIgnoreCase(value);
          }
        },
        new EntityCreator() {
          @Override
          public void create(String value) {
            Segment segment = new Segment();
            segment.setName(value);
            segments.save(segment);
          }
        },
        "segmentos");
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
    Optional<Buyer> buyerOptional = buyers.findById(id);
    if (buyerOptional.isPresent()) {
      Buyer buyer = buyerOptional.get();
      buyer.setName(name.trim());
      buyers.save(buyer);
      redirectAttributes.addFlashAttribute("successMessage", "Comprador atualizado.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado.");
    }
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
    Optional<Supplier> supplierOptional = suppliers.findById(id);
    if (supplierOptional.isPresent()) {
      Supplier supplier = supplierOptional.get();
      supplier.setName(name.trim());
      suppliers.save(supplier);
      redirectAttributes.addFlashAttribute("successMessage", "Fornecedor atualizado.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado.");
    }
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
    Optional<Segment> segmentOptional = segments.findById(id);
    if (segmentOptional.isPresent()) {
      Segment segment = segmentOptional.get();
      segment.setName(name.trim());
      segments.save(segment);
      redirectAttributes.addFlashAttribute("successMessage", "Segmento atualizado.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  @PostMapping("/compradores/{id}/excluir")
  public String deleteBuyer(@PathVariable Long id,
                            @RequestParam(name = "redirect", required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
    Optional<Buyer> buyerOptional = buyers.findById(id);
    if (buyerOptional.isPresent()) {
      buyers.delete(buyerOptional.get());
      redirectAttributes.addFlashAttribute("successMessage", "Comprador removido.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  @PostMapping("/fornecedores/{id}/excluir")
  public String deleteSupplier(@PathVariable Long id,
                               @RequestParam(name = "redirect", required = false) String redirect,
                               RedirectAttributes redirectAttributes) {
    Optional<Supplier> supplierOptional = suppliers.findById(id);
    if (supplierOptional.isPresent()) {
      suppliers.delete(supplierOptional.get());
      redirectAttributes.addFlashAttribute("successMessage", "Fornecedor removido.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  @PostMapping("/segmentos/{id}/excluir")
  public String deleteSegment(@PathVariable Long id,
                              @RequestParam(name = "redirect", required = false) String redirect,
                              RedirectAttributes redirectAttributes) {
    Optional<Segment> segmentOptional = segments.findById(id);
    if (segmentOptional.isPresent()) {
      segments.delete(segmentOptional.get());
      redirectAttributes.addFlashAttribute("successMessage", "Segmento removido.");
    } else {
      redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado.");
    }
    return redirectToCatalog(redirect);
  }

  private String importSimpleList(MultipartFile file,
                                  RedirectAttributes redirectAttributes,
                                  String redirect,
                                  EntityFinder finder,
                                  EntityCreator creator,
                                  String entityLabel) {
    int created = 0;
    List<String> warnings = new ArrayList<>();
    try {
      for (String[] row : csvImportService.parse(file)) {
        if (row.length == 0 || !StringUtils.hasText(row[0]) || row[0].toLowerCase().contains("nome")) {
          continue;
        }
        String name = row[0].trim();
        if (finder.findByName(name).isPresent()) {
          warnings.add("Já existia: " + name);
        } else {
          creator.create(name);
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

  private interface EntityFinder {
    Optional<?> findByName(String name);
  }

  private interface EntityCreator {
    void create(String name);
  }
}
