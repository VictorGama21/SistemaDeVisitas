package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Buyer;
importar com.inter.SistemaDeVisitas.entity.Segment;
importar com.inter.SistemaDeVisitas.entity.Supplier;
import com.inter.SistemaDeVisitas.repo.BuyerRepository;
import com.inter.SistemaDeVisitas.repo.SegmentRepository;
importar com.inter.SistemaDeVisitas.repo.SupplierRepository;
import com.inter.SistemaDeVisitas.service.CsvImportService;
importar org.springframework.dao.DataIntegrityViolationException;
importar org.springframework.data.domain.Page;
importar org.springframework.data.domain.PageRequest;
importar org.springframework.security.access.prepost.PreAuthorize;
importar org.springframework.stereotype.Controller;
importar org.springframework.ui.Model;
importar org.springframework.util.StringUtils;
importar org.springframework.web.bind.annotation.*;
importar org.springframework.web.multipart.MultipartFile;
importar org.springframework.web.servlet.mvc.support.RedirectAttributes;

importar java.io.IOException;
importar java.util.ArrayList;
importar java.util.List;
importar java.util.Opcional;
importar java.util.function.Consumer;
importar java.util.function.Function;

@Controlador
@RequestMapping("/admin/catálogos")
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
classe pública AdminCatalogController {
  

  compradores
 finais privados do BuyerRepository; 
  fornecedores finais privados do SupplierRepository;
 
  segmentos finais privados do SegmentRepository;
 
  privado final CsvImportService csvImportService;
 

  privado estático final int PAGE_SIZE = 10 ;
      

  público AdminCatalogController (compradores do BuyerRepository,
 
                                Fornecedores do SupplierRepository,
                                Segmentos do SegmentRepository,
                                Serviço de Importação Csv (Serviço de Importação Csv) {
    este .buyers = compradores;
    este .suppliers = fornecedores;
    este .segments = segmentos;
    este .csvImportService = csvImportService;
  }

  @GetMapping
  índice de String pública (modelo modelo,
                      @RequestParam(nome = "guia", defaultValue = "compradores") String activeTab,
                      @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                      @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                      @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage) {
 
    Page<Comprador> buyersPage = buyers.findAllByOrderByNameAsc(PageRequest.of(sanitizePage(buyerPage), PAGE_SIZE));
    Page<Fornecedor> suppliersPage = suppliers.findAllByOrderByNameAsc(PageRequest.of(sanitizePage(supplierPage), PAGE_SIZE));
    Page<Segmento> segmentsPage = segmentos.findAllByOrderByNameAsc(PageRequest.of(sanitizePage(segmentPage), PAGE_SIZE));

    modelo.addAttribute( "activeTab" , activeTab);
    modelo.addAttribute( "páginadocomprador" , páginadocomprador);
    model.addAttribute( "suppliersPage" , suppliersPage);
    modelo.addAttribute( "segmentosPage" , segmentosPage);
    modelo.addAttribute( "tamanho_da_página" , TAMANHO_DA_PÁGINA);
    retornar "admin/catálogos" ;
 
  }

  @PostMapping("/compradores")
  String pública createBuyer ( @RequestParam String nome,
                            @RequestParam(nome = "guia", defaultValue = "compradores") String guia,
                            @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                            @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                            @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                            Atributos de redirecionamento (atributos de redirecionamento) {
    se (!StringUtils.hasText(nome)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o comprador.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
    }
    compradores.findByNameIgnoreCase(name.trim()).ifPresentOrElse(existente -> {
      existente.setActive( verdadeiro );
      compradores.salvar(existente);
    }, () -> {
      Comprador comprador = novo Comprador ();
    
      comprador.setName(nome.trim());
      compradores.save(comprador);
    });
    redirectAttributes.addFlashAttribute("successMessage", "Comprador salvo com sucesso.");
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
  }

  @PostMapping("/fornecedores")
  String pública createSupplier ( @RequestParam String nome,
                               @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
                               @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                               @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                               @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                               Atributos de redirecionamento (atributos de redirecionamento) {
    se (!StringUtils.hasText(nome)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o fornecedor.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
    }
    fornecedores.findByNameIgnoreCase(name.trim()).ifPresentOrElse(existente -> {
      existente.setActive( verdadeiro );
      fornecedores.save(existente);
    }, () -> {
      Fornecedor fornecedor = novo Fornecedor ();
    
      fornecedor.setName(nome.trim());
      fornecedores.save(fornecedor);
    });
    redirectAttributes.addFlashAttribute("successMessage", "Fornecedor salvo com sucesso.");
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
  }

  @PostMapping("/segmentos")
  public String createSegment ( @RequestParam String nome,
                              @RequestParam(nome = "guia", defaultValue = "segmentos") String guia,
                              @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                              @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                              @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                              Atributos de redirecionamento (atributos de redirecionamento) {
    se (!StringUtils.hasText(nome)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome para o segmento.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
    }
    segmentos.findByNameIgnoreCase(name.trim()).ifPresentOrElse(existente -> {
      existente.setActive( verdadeiro );
      segmentos.save(existente);
    }, () -> {
      Segmento segmento = novo Segmento ();
    
      segmento.setName(nome.trim());
      segmentos.save(segmento);
    });
    redirectAttributes.addFlashAttribute("successMessage", "Segmento salvo com sucesso.");
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
  }

  @PostMapping("/compradores/{id}/status")
  public String toggleBuyer ( @PathVariable ID longo,
                            @RequestParam(nome = "guia", defaultValue = "compradores") String guia,
                            @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                            @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                            @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage) {
 
    compradores.findById(id).ifPresent(comprador -> {
      comprador.setActive(!comprador.isActive());
      compradores.save(comprador);
    });
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
  }

  @PostMapping("/fornecedores/{id}/status")
  String pública toggleSupplier ( @PathVariable ID longo,
                               @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
                               @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                               @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                               @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage) {
 
    fornecedores.findById(id).ifPresent(fornecedor -> {
      fornecedor.setActive(!supplier.isActive());
      fornecedores.save(fornecedor);
    });
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
  }

  @PostMapping("/segmentos/{id}/status")
  String pública toggleSegment ( @PathVariable ID longo,
                              @RequestParam(nome = "guia", defaultValue = "segmentos") String guia,
                              @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                              @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                              @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage) {
 
    segmentos.findById(id).ifPresent(segmento -> {
      segmento.setActive(!segmento.isActive());
      segmentos.save(segmento);
    });
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/compradores/{id}/editar")
  public String updateBuyer ( @PathVariable ID longo,
                            @RequestParam Nome da string,
                            @RequestParam(nome = "guia", defaultValue = "compradores") String guia,
                            @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                            @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                            @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                            Atributos de redirecionamento (atributos de redirecionamento) {
    se (!StringUtils.hasText(nome)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome válido para o comprador.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
    }
    String aparada = nome.trim();
  
    Optional<Buyer> duplicado = compradores.findByNameIgnoreCase(aparado);
    se (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Já existe um comprador com esse nome.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
    }
    compradores.findById(id).ifPresentOrElse(comprador -> {
      comprador.setName(aparado);
      compradores.save(comprador);
      redirectAttributes.addFlashAttribute("successMessage", "Comprador atualizado com sucesso.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Comprador não encontrado."));
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/fornecedores/{id}/editar")
  String pública updateSupplier ( @PathVariable ID longo,
                               @RequestParam Nome da string,
                               @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
                               @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                               @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                               @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                               Atributos de redirecionamento (atributos de redirecionamento) {
    se (!StringUtils.hasText(nome)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome válido para o fornecedor.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
    }
    String aparada = nome.trim();
  
    Optional<Fornecedor> duplicado = fornecedores.findByNameIgnoreCase(aparado);
    se (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Já existe um fornecedor com esse nome.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
    }
    fornecedores.findById(id).ifPresentOrElse(fornecedor -> {
      fornecedor.setName(aparado);
      fornecedores.save(fornecedor);
      redirectAttributes.addFlashAttribute("successMessage", "Fornecedor atualizado com sucesso.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Fornecedor não encontrado."));
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/segmentos/{id}/editar")
  String pública updateSegment ( @PathVariable ID longo,
                              @RequestParam Nome da string,
                              @RequestParam(nome = "guia", defaultValue = "segmentos") String guia,
                              @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                              @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                              @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                              Atributos de redirecionamento (atributos de redirecionamento) {
    se (!StringUtils.hasText(nome)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Informe um nome válido para o segmento.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
    }
    String aparada = nome.trim();
  
    Optional<Segment> duplicado = segmentos.findByNameIgnoreCase(aparado);
    se (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Já existe um segmento com esse nome.");
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
    }
    segmentos.findById(id).ifPresentOrElse(segmento -> {
      segmento.setName(aparado);
      segmentos.save(segmento);
      redirectAttributes.addFlashAttribute("successMessage", "Segmento atualizado com sucesso.");
    }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Segmento não encontrado."));
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/compradores/{id}/excluir")
  public String deleteBuyer ( @PathVariable ID longo,
                            @RequestParam(nome = "guia", defaultValue = "compradores") String guia,
                            @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                            @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                            @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                            Atributos de redirecionamento (atributos de redirecionamento) {
    tentar {
      compradores.deleteById(id);
      redirectAttributes.addFlashAttribute("successMessage", "Comprador excluído com sucesso.");
    } pegar (DataIntegrityViolationException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível excluir o comprador: existem visitas associadas.");
    }
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/fornecedores/{id}/excluir")
  String pública deleteSupplier ( @PathVariable ID longo,
                               @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
                               @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                               @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                               @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                               Atributos de redirecionamento (atributos de redirecionamento) {
    tentar {
      fornecedores.deleteById(id);
      redirectAttributes.addFlashAttribute("successMessage", "Fornecedor excluído com sucesso.");
    } pegar (DataIntegrityViolationException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível excluir o fornecedor: existem visitas associadas.");
    }
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/segmentos/{id}/excluir")
  public String deleteSegment ( @PathVariable ID longo,
                              @RequestParam(nome = "guia", defaultValue = "segmentos") String guia,
                              @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                              @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                              @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                              Atributos de redirecionamento (atributos de redirecionamento) {
    tentar {
      segmentos.deleteById(id);
      redirectAttributes.addFlashAttribute("successMessage", "Segmento excluído com sucesso.");
    } pegar (DataIntegrityViolationException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível excluir o segmento: existem visitas associadas.");
    }
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  @PostMapping("/compradores/importação")
  public String importBuyers ( @RequestParam("file") arquivo MultipartFile,
                             @RequestParam(nome = "guia", defaultValue = "compradores") String guia,
                             @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                             @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                             @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                             Atributos de redirecionamento (atributos de redirecionamento) {
    retornar importSimpleList(arquivo, redirectAttributes, compradores::findByNameIgnoreCase, nome -> {
      Comprador comprador = novo Comprador ();
    
      comprador.setName(nome);
      compradores.save(comprador);
    }, "compradores" , tab, buyerPage, supplierPage, segmentPage );
  }

  @PostMapping("/fornecedores/importar")
  public String importSuppliers ( @RequestParam("file") MultipartFile arquivo,
                                @RequestParam(name = "tab", defaultValue = "suppliers") String tab,
                                @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                                @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                                @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                                Atributos de redirecionamento (atributos de redirecionamento) {
    retornar importSimpleList(arquivo, redirectAttributes, fornecedores::findByNameIgnoreCase, nome -> {
      Fornecedor fornecedor = novo Fornecedor ();
    
      fornecedor.setName(nome);
      fornecedores.save(fornecedor);
    }, "fornecedores" , tab, buyerPage, supplierPage, segmentPage );
  }

  @PostMapping("/segmentos/importar")
  public String importSegments ( @RequestParam("file") MultipartFile arquivo,
                               @RequestParam(nome = "guia", defaultValue = "segmentos") String guia,
                               @RequestParam(nome = "buyerPage", defaultValue = "0") int buyerPage,
 
                               @RequestParam(nome = "supplierPage", defaultValue = "0") int supplierPage,
 
                               @RequestParam(nome = "segmentoPage", valor padrão = "0") int segmentoPage,
 
                               Atributos de redirecionamento (atributos de redirecionamento) {
    retornar importSimpleList(arquivo, redirectAttributes, segmentos::findByNameIgnoreCase, nome -> {
      Segmento segmento = novo Segmento ();
    
      segmento.setName(nome);
      segmentos.save(segmento);
    }, "segmentos" , aba, buyerPage, supplierPage, segmentPage );
  }

  String privada importSimpleList (arquivo MultipartFile,
                                  Atributos de redirecionamento Atributos de redirecionamento,
                                  Função<String, Opcional<?>> localizador,
                                  Criador do consumidor<String>,
                                  Entidade de stringLabel,
                                  Tabulação de cordas,
                                  int compradorPage,
                                  int fornecedorPage,
                                  int segmentoPágina) {
    int criado = 0 ;
   
    List<String> avisos = novo ArrayList <>();
 
    tentar {
      para (String[] linha: csvImportService.parse(arquivo)) {
        se (linha.length == 0 || !StringUtils.hasText(linha[ 0 ]) || linha[ 0 ].toLowerCase().contains( "nome" )) {
          continuar ;
        }
        Nome da string = linha[ 0 ].trim();
  
        se (finder.apply(nome).isPresent()) {
          warnings.add("Já existia: " + name);
        } outro {
          criador.aceitar(nome);
          criado++;
        }
      }
    } catch (IOException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Erro ao importar " + entityLabel + ": " + e.getMessage());
      retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage) ;
 
    }

    se (criado > 0 ) {
      redirectAttributes.addFlashAttribute( "successMessage" , criado + " " + entityLabel + " importado(s)." );
    }
    se (! avisos. está vazio ()) {
      redirectAttributes.addFlashAttribute( "warningMessages" , avisos);
    }
    retornar redirectToCatalog(guia, buyerPage, supplierPage, segmentPage);
  }

  privado int sanitizePage ( int página) {
  
    retornar Math.max(página, 0 );
  }

  String privada redirectToCatalog (String tab, int buyerPage, int supplierPage, int segmentPage) {
    retornar "redirecionamento:/admin/catalogos?tab=" + tab
 
        + "&buyerPage=" + sanitizePage(buyerPage)
        + "&supplierPage=" + sanitizePage(supplierPage)
        + "&segmentPage=" + sanitizePage(segmentPage);
  }
}
