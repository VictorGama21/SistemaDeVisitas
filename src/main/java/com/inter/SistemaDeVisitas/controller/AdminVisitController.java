package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.*;
import com.inter.SistemaDeVisitas.repo.*;
import com.inter.SistemaDeVisitas.service.CsvImportService;
import com.inter.SistemaDeVisitas.service.VisitAnalyticsService;
import com.inter.SistemaDeVisitas.service.VisitExportService;
import com.inter.SistemaDeVisitas.service.VisitFilterCriteria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final VisitAnalyticsService visitAnalyticsService;
    private final VisitExportService visitExportService;

    public AdminVisitController(VisitRepository visits,
                                StoreRepository stores,
                                UserRepository users,
                                BuyerRepository buyers,
                                SupplierRepository suppliers,
                                SegmentRepository segments,
                                CsvImportService csvImportService,
                                VisitAnalyticsService visitAnalyticsService,
                                VisitExportService visitExportService) {
        this.visits = visits;
        this.stores = stores;
        this.users = users;
        this.buyers = buyers;
        this.suppliers = suppliers;
        this.segments = segments;
        this.csvImportService = csvImportService;
        this.visitAnalyticsService = visitAnalyticsService;
        this.visitExportService = visitExportService;
    }

    private FilterContext resolveFilterContext(Long storeId,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               String visitStatus,
                                               String modality,
                                               Long buyerId,
                                               Long supplierId,
                                               Long segmentId,
                                               String dayFilter) {
        Store selectedStore = null;
        if (storeId != null) {
            selectedStore = stores.findById(storeId).orElse(null);
        }
        LocalDate normalizedStart = startDate;
        LocalDate normalizedEnd = endDate;
        if (normalizedStart != null && normalizedEnd != null && normalizedEnd.isBefore(normalizedStart)) {
            LocalDate swap = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = swap;
        }
        VisitFilterCriteria criteria = buildCriteria(dayFilter, visitStatus, modality, buyerId, supplierId, segmentId);
        boolean hasAnyFilter = normalizedStart != null || normalizedEnd != null || selectedStore != null
            || criteria.hasStatusFilter() || criteria.hasModalityFilter()
            || criteria.hasBuyerFilter() || criteria.hasSupplierFilter()
            || criteria.hasSegmentFilter() || criteria.hasDayOfWeekFilter();
        String rawDayFilter = Optional.ofNullable(dayFilter).orElse("todos");
        return new FilterContext(selectedStore, normalizedStart, normalizedEnd, criteria, hasAnyFilter, rawDayFilter);
    }

    private VisitFilterCriteria buildCriteria(String dayFilter,
                                              String visitStatus,
                                              String modality,
                                              Long buyerId,
                                              Long supplierId,
                                              Long segmentId) {
        VisitFilterCriteria.Builder builder = VisitFilterCriteria.builder();
        DayOfWeek dayOfWeek = parseDayFilter(dayFilter);
        if (dayOfWeek != null) {
            builder.dayOfWeek(dayOfWeek);
        }
        builder.addStatus(parseVisitStatus(visitStatus));
        builder.addModality(parseVisitModality(modality));
        builder.buyerId(buyerId);
        builder.supplierId(supplierId);
        builder.segmentId(segmentId);
        return builder.build();
    }

    private VisitStatus parseVisitStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return VisitStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private VisitModality parseVisitModality(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return VisitModality.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private DayOfWeek parseDayFilter(String input) {
        if (input == null || input.isBlank() || "todos".equalsIgnoreCase(input)) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "segunda" -> DayOfWeek.MONDAY;
            case "terca", "terça" -> DayOfWeek.TUESDAY;
            case "quarta" -> DayOfWeek.WEDNESDAY;
            case "quinta" -> DayOfWeek.THURSDAY;
            case "sexta" -> DayOfWeek.FRIDAY;
            case "sabado", "sábado" -> DayOfWeek.SATURDAY;
            case "domingo" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private List<String> buildDayFilterOptions() {
        return List.of("todos", "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo");
    }

    private String buildExportFileName(Store store) {
        String base = store != null ? store.getName() : "todas_as_lojas";
        if (base == null || base.isBlank()) {
            base = "todas_as_lojas";
        }
        String normalized = base.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.ROOT);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        return "visitas_" + normalized + "_" + timestamp + ".xlsx";
    }

    private record FilterContext(Store store,
                                 LocalDate start,
                                 LocalDate end,
                                 VisitFilterCriteria criteria,
                                 boolean hasAnyFilter,
                                 String rawDayFilter) {
    }

    @GetMapping
    public String list(@RequestParam(name = "storeId", required = false) Long storeId,
                       @RequestParam(name = "inicio", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(name = "fim", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(name = "visitStatus", required = false) String visitStatus,
                       @RequestParam(name = "modalidade", required = false) String modality,
                       @RequestParam(name = "buyerId", required = false) Long buyerId,
                       @RequestParam(name = "supplierId", required = false) Long supplierId,
                       @RequestParam(name = "segmentId", required = false) Long segmentId,
                       @RequestParam(name = "dia", required = false) String dayFilter,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "size", defaultValue = "20") int size,
                       Model model,
                       HttpServletRequest request) {
        List<Store> storeList = stores.findAllByOrderByNameAsc();
        List<Store> activeStores = stores.findByActiveTrueOrderByNameAsc();
        FilterContext context = resolveFilterContext(storeId, startDate, endDate, visitStatus, modality, buyerId, supplierId, segmentId, dayFilter);

        LocalDate effectiveStart = context.start();
        LocalDate effectiveEnd = context.end();
        boolean defaultRangeApplied = false;

        if (!context.hasAnyFilter()) {
            LocalDate today = LocalDate.now();
            effectiveStart = today.minusDays(30);
            effectiveEnd = today.plusDays(30);
            defaultRangeApplied = true;
        }

        List<Visit> loadedVisits = visitAnalyticsService.loadVisits(context.store(), effectiveStart, effectiveEnd);

        List<Visit> filteredVisits = visitAnalyticsService.applyFilters(loadedVisits, context.criteria());

        int pageSize = Math.max(5, Math.min(size, 100));
        int totalVisits = filteredVisits.size();
        int totalPages = (int) Math.ceil(totalVisits / (double) pageSize);
        int currentPage = Math.max(0, Math.min(page, Math.max(totalPages - 1, 0)));
        int fromIndex = Math.min(currentPage * pageSize, totalVisits);
        int toIndex = Math.min(fromIndex + pageSize, totalVisits);
        List<Visit> pageContent = filteredVisits.subList(fromIndex, toIndex);
        List<Visit> detailedVisits = List.copyOf(filteredVisits);

        model.addAttribute("stores", storeList);
        model.addAttribute("availableStores", activeStores);
        model.addAttribute("visits", pageContent);
        model.addAttribute("detailedVisits", detailedVisits);
        model.addAttribute("selectedStore", context.store());
        model.addAttribute("startDate", effectiveStart);
        model.addAttribute("endDate", effectiveEnd);
        model.addAttribute("buyers", buyers.findByActiveTrueOrderByNameAsc());
        model.addAttribute("suppliers", suppliers.findByActiveTrueOrderByNameAsc());
        model.addAttribute("segments", segments.findByActiveTrueOrderByNameAsc());
        model.addAttribute("modalities", VisitModality.values());
        model.addAttribute("statusOptions", VisitStatus.values());
        model.addAttribute("dayOptions", buildDayFilterOptions());
        model.addAttribute("selectedVisitStatus", visitStatus);
        model.addAttribute("selectedModality", modality);
        model.addAttribute("selectedBuyerId", buyerId);
        model.addAttribute("selectedSupplierId", supplierId);
        model.addAttribute("selectedSegmentId", segmentId);
        model.addAttribute("selectedDayFilter", context.rawDayFilter());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalFilteredVisits", totalVisits);
        model.addAttribute("currentQuery", request.getQueryString() == null ? "" : request.getQueryString());
        model.addAttribute("filtersApplied", context.hasAnyFilter() || defaultRangeApplied);
        return "admin/visitas";
    }

    @GetMapping("/exportar")
    public ResponseEntity<ByteArrayResource> export(@RequestParam(name = "storeId", required = false) Long storeId,
                                                    @RequestParam(name = "inicio", required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                    @RequestParam(name = "fim", required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                    @RequestParam(name = "visitStatus", required = false) String visitStatus,
                                                    @RequestParam(name = "modalidade", required = false) String modality,
                                                    @RequestParam(name = "buyerId", required = false) Long buyerId,
                                                    @RequestParam(name = "supplierId", required = false) Long supplierId,
                                                    @RequestParam(name = "segmentId", required = false) Long segmentId,
                                                    @RequestParam(name = "dia", required = false) String dayFilter) throws IOException {
        FilterContext context = resolveFilterContext(storeId, startDate, endDate, visitStatus, modality, buyerId, supplierId, segmentId, dayFilter);
        List<Visit> baseVisits = visitAnalyticsService.loadVisits(context.store(), context.start(), context.end());
        List<Visit> filteredVisits = visitAnalyticsService.applyFilters(baseVisits, context.criteria());
        byte[] file = visitExportService.export(filteredVisits, context.start(), context.end(), context.store(), context.criteria());
        String filename = buildExportFileName(context.store());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .contentLength(file.length)
            .body(new ByteArrayResource(file));
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
        visit.setLastStatusUpdatedBy(creator);
        visit.setLastStatusUpdatedAt(Instant.now());
        
        visits.save(visit);
        redirectAttributes.addFlashAttribute("successMessage",
            "Visita agendada para " + scheduledDate.format(IMPORT_DATE_FORMAT) + " em " + selectedStores.size() + " loja(s).");
        return "redirect:/admin/visitas";
    }
    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id,
                           @RequestParam(name = "redirect", required = false) String redirect,
                           Model model) {
        Visit visit = visits.findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("visit", visit);
        model.addAttribute("stores", stores.findAllByOrderByNameAsc());
        model.addAttribute("availableStores", stores.findByActiveTrueOrderByNameAsc());
        model.addAttribute("buyers", buyers.findByActiveTrueOrderByNameAsc());
        model.addAttribute("suppliers", suppliers.findByActiveTrueOrderByNameAsc());
        model.addAttribute("segments", segments.findByActiveTrueOrderByNameAsc());
        model.addAttribute("modalities", VisitModality.values());
        model.addAttribute("statusOptions", VisitStatus.values());
        model.addAttribute("redirectQuery", redirect);
        return "admin/visita-editar";
    }

    @PostMapping("/{id}/editar")
    public String update(@PathVariable Long id,
                         @RequestParam(value = "storeIds", required = false) List<Long> storeIds,
                         @RequestParam("scheduledDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
                         @RequestParam("status") VisitStatus status,
                         @RequestParam("modality") String modality,
                         @RequestParam(required = false) String buyerName,
                         @RequestParam(required = false) String supplierName,
                         @RequestParam(required = false) String segmentName,
                         @RequestParam(required = false) String commercialInfo,
                         @RequestParam(required = false) String comment,
                         @RequestParam(name = "rating", required = false) String ratingInput,
                         @RequestParam(name = "redirect", required = false) String redirect,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        Visit visit = visits.findDetailedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (storeIds == null || storeIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selecione ao menos uma loja.");
            return buildEditRedirect(id, redirect);
        }
        List<Store> selectedStores = stores.findAllById(storeIds);
        if (selectedStores.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nenhuma loja válida foi encontrada.");
            return buildEditRedirect(id, redirect);
        }
        visit.setStores(new LinkedHashSet<>(selectedStores));

        visit.setScheduledDate(scheduledDate);
        VisitStatus previousStatus = visit.getStatus();
        visit.setStatus(status);
        visit.setModality(VisitModality.fromString(modality));

        Buyer buyer = resolveBuyer(buyerName, redirectAttributes);
        if (StringUtils.hasText(buyerName) && buyer == null) {
            return buildEditRedirect(id, redirect);
        }
        visit.setBuyer(buyer);

        Supplier supplier = resolveSupplier(supplierName, redirectAttributes);
        if (StringUtils.hasText(supplierName) && supplier == null) {
            return buildEditRedirect(id, redirect);
        }
        visit.setSupplier(supplier);

        Segment segment = resolveSegment(segmentName, redirectAttributes);
        if (StringUtils.hasText(segmentName) && segment == null) {
            return buildEditRedirect(id, redirect);
        }
        visit.setSegment(segment);

        visit.setCommercialInfo(StringUtils.hasText(commercialInfo) ? commercialInfo.trim() : null);
        visit.setComment(StringUtils.hasText(comment) ? comment.trim() : null);

        Integer rating = null;
        if (StringUtils.hasText(ratingInput)) {
            try {
                rating = Integer.parseInt(ratingInput.trim());
            } catch (NumberFormatException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Informe uma nota numérica entre 1 e 5.");
                return buildEditRedirect(id, redirect);
            }
            if (rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("errorMessage", "A nota deve estar entre 1 e 5.");
                return buildEditRedirect(id, redirect);
            }
        }
        visit.setRating(rating);
        if (previousStatus != status) {
            User editor = users.findByEmail(authentication.getName()).orElseThrow();
            visit.setLastStatusUpdatedBy(editor);
            visit.setLastStatusUpdatedAt(Instant.now());
        }
        
        visits.save(visit);
        redirectAttributes.addFlashAttribute("successMessage", "Visita atualizada com sucesso.");
        return redirectToList(redirect);
    }

    @PostMapping("/{id}/excluir")
    public String delete(@PathVariable Long id,
                         @RequestParam(name = "redirect", required = false) String redirect,
                         RedirectAttributes redirectAttributes) {
        if (!visits.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "A visita informada não foi encontrada.");
            return redirectToList(redirect);
        }
        visits.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Visita excluída com sucesso.");
        return redirectToList(redirect);
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
    private String redirectToList(String redirect) {
        return "redirect:/admin/visitas" + buildRedirectSuffix(redirect);
    }

    private String buildRedirectSuffix(String redirect) {
        if (!StringUtils.hasText(redirect)) {
            return "";
        }
        return "?" + redirect;
    }

    private String buildEditRedirect(Long id, String redirect) {
        String base = "redirect:/admin/visitas/" + id + "/editar";
        if (!StringUtils.hasText(redirect)) {
            return base;
        }
        String encoded = UriUtils.encode(redirect, StandardCharsets.UTF_8);
        return base + "?redirect=" + encoded;
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
        visit.setLastStatusUpdatedBy(creator);
        visit.setLastStatusUpdatedAt(Instant.now());
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
