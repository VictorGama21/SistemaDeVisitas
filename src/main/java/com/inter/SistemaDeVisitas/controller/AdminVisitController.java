package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.*;
import com.inter.SistemaDeVisitas.repo.*;
import com.inter.SistemaDeVisitas.service.CsvImportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/visitas")
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
public class AdminVisitController {

    private static final DateTimeFormatter IMPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VisitRepository visits;
    private final StoreRepository stores;
    private final UserRepository users;
    private final BuyerRepository buyers;
    private final SupplierRepository suppliers;
    private final SegmentRepository segments;
    private final CsvImportService csvImportService;

    public AdminVisitController(VisitRepository visits,
                                StoreRepository stores,
                                UserRepository users,
                                BuyerRepository buyers,
                                SupplierRepository suppliers,
                                SegmentRepository segments,
                                CsvImportService csvImportService) {
        this.visits = visits;
        this.stores = stores;
        this.users = users;
        this.buyers = buyers;
        this.suppliers = suppliers;
        this.segments = segments;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public String list(@RequestParam(name = "storeId", required = false) Long storeId,
                       Model model) {
        List<Store> storeList = stores.findAllByOrderByNameAsc();
        List<Store> activeStores = stores.findByActiveTrueOrderByNameAsc();
        List<Visit> visitList;
        Store selectedStore = null;

        if (storeId != null) {
            selectedStore = stores.findById(storeId).orElse(null);
        }

        if (selectedStore != null) {
            visitList = visits.findByStoreOrderByScheduledDateDesc(selectedStore);
        } else {
            visitList = visits.findTop10ByOrderByScheduledDateDesc();
        }

        model.addAttribute("stores", storeList);
        model.addAttribute("availableStores", activeStores);
        model.addAttribute("visits", visitList);
        model.addAttribute("selectedStore", selectedStore);
        model.addAttribute("buyers", buyers.findByActiveTrueOrderByNameAsc());
        model.addAttribute("suppliers", suppliers.findByActiveTrueOrderByNameAsc());
        model.addAttribute("segments", segments.findByActiveTrueOrderByNameAsc());
        model.addAttribute("modalities", VisitModality.values());
        return "admin/visitas";
    }

    @PostMapping
    public String schedule(@RequestParam(value = "storeIds", required = false) List<Long> storeIds,
                           @RequestParam("scheduledDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
                           @RequestParam(required = false) String buyerName,
                           @RequestParam(required = false) String supplierName,
                           @RequestParam(required = false) String segmentName,
                           @RequestParam("modality") String modality,
                           @RequestParam(required = false) String commercialInfo,
                           @RequestParam(required = false) String comment,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (storeIds == null || storeIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selecione ao menos uma loja.");
            return "redirect:/admin/visitas";
        }

        List<Store> selectedStores = stores.findAllById(storeIds).stream()
            .filter(Store::isActive)
            .collect(Collectors.toList());

        if (selectedStores.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nenhuma das lojas selecionadas está ativa.");
            return "redirect:/admin/visitas";
        }

        Buyer buyer = resolveBuyer(buyerName, redirectAttributes);
        if (buyerName != null && !buyerName.isBlank() && buyer == null) {
            return "redirect:/admin/visitas";
        }

        Supplier supplier = resolveSupplier(supplierName, redirectAttributes);
        if (supplierName != null && !supplierName.isBlank() && supplier == null) {
            return "redirect:/admin/visitas";
        }

        Segment segment = resolveSegment(segmentName, redirectAttributes);
        if (segmentName != null && !segmentName.isBlank() && segment == null) {
            return "redirect:/admin/visitas";
        }

        Visit visit = new Visit();
        visit.setStores(new LinkedHashSet<>(selectedStores));
        visit.setScheduledDate(scheduledDate);
        visit.setBuyer(buyer);
        visit.setSupplier(supplier);
        visit.setSegment(segment);
        visit.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
        visit.setCommercialInfo(StringUtils.hasText(commercialInfo) ? commercialInfo.trim() : null);
        visit.setModality(VisitModality.fromString(modality));
        User creator = users.findByEmail(authentication.getName()).orElseThrow();
        visit.setCreatedBy(creator);

        visits.save(visit);
        redirectAttributes.addFlashAttribute("successMessage",
            "Visita agendada para " + scheduledDate.format(IMPORT_DATE_FORMAT) + " em " + selectedStores.size() + " loja(s).");
        return "redirect:/admin/visitas";
    }

    @PostMapping("/importar")
    public String importVisits(@RequestParam("file") MultipartFile file,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        int created = 0;
        List<String> errors = new ArrayList<>();
        User creator = users.findByEmail(authentication.getName()).orElseThrow();
        try {
            List<String[]> rows = csvImportService.parse(file);
            if (rows.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "O arquivo enviado está vazio.");
                return "redirect:/admin/visitas";
            }

            for (int i = 0; i < rows.size(); i++) {
                String[] columns = rows.get(i);
                if (i == 0 && isHeader(columns)) {
                    continue;
                }
                if (columns.length < 2) {
                    errors.add("Linha " + (i + 1) + ": formato inválido.");
                    continue;
                }
                try {
                    Visit visit = buildVisitFromRow(columns, creator);
                    visits.save(visit);
                    created++;
                } catch (IllegalArgumentException ex) {
                    errors.add("Linha " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Falha ao ler o arquivo: " + e.getMessage());
            return "redirect:/admin/visitas";
        }

        if (created > 0) {
            redirectAttributes.addFlashAttribute("successMessage", created + " visita(s) importada(s) com sucesso.");
        }
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("warningMessages", errors);
        }
        return "redirect:/admin/visitas";
    }

    private boolean isHeader(String[] columns) {
        String first = columns[0].toLowerCase(Locale.ROOT);
        return first.contains("data") && columns.length > 1;
    }

    private Visit buildVisitFromRow(String[] columns, User creator) {
        LocalDate date;
        try {
            date = LocalDate.parse(columns[0], IMPORT_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + columns[0]);
        }

        if (columns.length < 2 || columns[1].isBlank()) {
            throw new IllegalArgumentException("Informe ao menos uma loja");
        }
        Set<Store> selectedStores = Arrays.stream(columns[1].split("\\|"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(name -> stores.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalArgumentException("Loja não encontrada: " + name)))
            .filter(Store::isActive)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (selectedStores.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma loja ativa encontrada para a linha.");
        }

        String buyerName = columns.length > 2 ? columns[2] : null;
        String supplierName = columns.length > 3 ? columns[3] : null;
        String segmentName = columns.length > 4 ? columns[4] : null;
        String modality = columns.length > 5 ? columns[5] : null;
        String commercialInfo = columns.length > 6 ? columns[6] : null;
        String comment = columns.length > 7 ? columns[7] : null;

        Visit visit = new Visit();
        visit.setStores(selectedStores);
        visit.setScheduledDate(date);

        Buyer buyer = resolveBuyer(buyerName, null);
        if (StringUtils.hasText(buyerName) && buyer == null) {
            throw new IllegalArgumentException("Comprador não cadastrado: " + buyerName);
        }
        visit.setBuyer(buyer);

        Supplier supplier = resolveSupplier(supplierName, null);
        if (StringUtils.hasText(supplierName) && supplier == null) {
            throw new IllegalArgumentException("Fornecedor não cadastrado: " + supplierName);
        }
        visit.setSupplier(supplier);

        Segment segment = resolveSegment(segmentName, null);
        if (StringUtils.hasText(segmentName) && segment == null) {
            throw new IllegalArgumentException("Segmento não cadastrado: " + segmentName);
        }
        visit.setSegment(segment);

        visit.setModality(VisitModality.fromString(modality));
        visit.setCommercialInfo(StringUtils.hasText(commercialInfo) ? commercialInfo : null);
        visit.setComment(StringUtils.hasText(comment) ? comment : null);
        visit.setCreatedBy(creator);
        return visit;
    }

    private Buyer resolveBuyer(String name, RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return buyers.findByNameIgnoreCase(name.trim())
            .orElseGet(() -> {
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Comprador não cadastrado: " + name);
                }
                return null;
            });
    }

    private Supplier resolveSupplier(String name, RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return suppliers.findByNameIgnoreCase(name.trim())
            .orElseGet(() -> {
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não cadastrado: " + name);
                }
                return null;
            });
    }

    private Segment resolveSegment(String name, RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return segments.findByNameIgnoreCase(name.trim())
            .orElseGet(() -> {
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Segmento não cadastrado: " + name);
                }
                return null;
            });
    }
}
