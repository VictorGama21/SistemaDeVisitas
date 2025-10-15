package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Buyer;
import com.inter.SistemaDeVisitas.entity.Segment;
import com.inter.SistemaDeVisitas.entity.Supplier;
import com.inter.SistemaDeVisitas.repo.BuyerRepository;
import com.inter.SistemaDeVisitas.repo.SegmentRepository;
import com.inter.SistemaDeVisitas.repo.SupplierRepository;
import com.inter.SistemaDeVisitas.service.CsvImportService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private static final int PAGE_SIZE = 10;

    public AdminCatalogController(
            BuyerRepository buyers,
            SupplierRepository suppliers,
            SegmentRepository segments,
            CsvImportService csvImportService
    ) {
        this.buyers = buyers;
        this.suppliers = suppliers;
        this.segments = segments;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public String index(
            Model model,
            @RequestParam(name = "tab", defaultValue = "buyers") String activeTab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage
    ) {
        Page<Buyer> buyersPage = buyers.findAllByOrderByNameAsc(PageRequest.of(sanitizePage(buyerPage), PAGE_SIZE));
        Page<Supplier> suppliersPage = suppliers.findAllByOrderByNameAsc(PageRequest.of(sanitizePage(supplierPage), PAGE_SIZE));
        Page<Segment> segmentsPage = segments.findAllByOrderByNameAsc(PageRequest.of(sanitizePage(segmentPage), PAGE_SIZE));

        model.addAttribute("activeTab", activeTab);
        model.addAttribute("buyersPage", buyersPage);
        model.addAttribute("suppliersPage", suppliersPage);
        model.addAttribute("segmentsPage", segmentsPage);
        model.addAttribute("pageSize", PAGE_SIZE);

        return "admin/catalogos";
    }

    /* -------------------- CRIAÇÃO -------------------- */

    @PostMapping("/compradores")
    public String createBuyer(
            @RequestParam String name,
            @RequestParam(name = "tab", defaultValue = "buyers") String tab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage,
            RedirectAttributes redirectAttributes
    ) {
        if (!StringUtils.hasText(name)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o comprador.");
            return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
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
        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    @PostMapping("/fornecedores")
    public String createSupplier(
            @RequestParam String name,
            @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage,
            RedirectAttributes redirectAttributes
    ) {
        if (!StringUtils.hasText(name)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o fornecedor.");
            return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
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
        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    @PostMapping("/segmentos")
    public String createSegment(
            @RequestParam String name,
            @RequestParam(name = "tab", defaultValue = "segments") String tab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage,
            RedirectAttributes redirectAttributes
    ) {
        if (!StringUtils.hasText(name)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o segmento.");
            return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
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
        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    /* -------------------- ALTERAR STATUS -------------------- */

    @PostMapping("/compradores/{id}/status")
    public String toggleBuyer(
            @PathVariable Long id,
            @RequestParam(name = "tab", defaultValue = "buyers") String tab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage
    ) {
        buyers.findById(id).ifPresent(buyer -> {
            buyer.setActive(!buyer.isActive());
            buyers.save(buyer);
        });
        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    @PostMapping("/fornecedores/{id}/status")
    public String toggleSupplier(
            @PathVariable Long id,
            @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage
    ) {
        suppliers.findById(id).ifPresent(supplier -> {
            supplier.setActive(!supplier.isActive());
            suppliers.save(supplier);
        });
        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    @PostMapping("/segmentos/{id}/status")
    public String toggleSegment(
            @PathVariable Long id,
            @RequestParam(name = "tab", defaultValue = "segments") String tab,
            @RequestParam(name = "buyerPage", defaultValue = "0") int buyerPage,
            @RequestParam(name = "supplierPage", defaultValue = "0") int supplierPage,
            @RequestParam(name = "segmentPage", defaultValue = "0") int segmentPage
    ) {
        segments.findById(id).ifPresent(segment -> {
            segment.setActive(!segment.isActive());
            segments.save(segment);
        });
        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    /* -------------------- IMPORTAÇÃO CSV -------------------- */

    private String importSimpleList(
            MultipartFile file,
            RedirectAttributes redirectAttributes,
            Function<String, Optional<?>> finder,
            Consumer<String> creator,
            String entityLabel,
            String tab,
            int buyerPage,
            int supplierPage,
            int segmentPage
    ) {
        int created = 0;
        List<String> warnings = new ArrayList<>();

        try {
            for (String[] line : csvImportService.parse(file)) {
                if (line.length == 0 || !StringUtils.hasText(line[0]) || line[0].toLowerCase().contains("nome")) {
                    continue;
                }
                String name = line[0].trim();

                if (finder.apply(name).isPresent()) {
                    warnings.add("Já existia: " + name);
                } else {
                    creator.accept(name);
                    created++;
                }
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao importar " + entityLabel + ": " + e.getMessage());
            return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
        }

        if (created > 0) {
            redirectAttributes.addFlashAttribute("successMessage", created + " " + entityLabel + "(s) importado(s).");
        }
        if (!warnings.isEmpty()) {
            redirectAttributes.addFlashAttribute("warningMessages", warnings);
        }

        return redirectToCatalog(tab, buyerPage, supplierPage, segmentPage);
    }

    /* -------------------- UTILITÁRIOS -------------------- */

    private int sanitizePage(int page) {
        return Math.max(page, 0);
    }

    private String redirectToCatalog(String tab, int buyerPage, int supplierPage, int segmentPage) {
        return "redirect:/admin/catalogos?tab=" + tab
                + "&buyerPage=" + sanitizePage(buyerPage)
                + "&supplierPage=" + sanitizePage(supplierPage)
                + "&segmentPage=" + sanitizePage(segmentPage);
    }
}
