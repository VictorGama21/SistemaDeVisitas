# 🧾 Sistema de Visity

Um sistema web desenvolvido em **Spring Boot 3.5.5** para o controle e acompanhamento de visitas às lojas da rede Quitandaria e unidades do Grupo Verdão.  
A aplicação permite o registro, monitoramento e exportação de relatórios das visitas realizadas por gestores e equipes internas.

---

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.5.5**
  - Spring MVC  
  - Spring Data JPA  
  - Spring Security  
- **Thymeleaf** (camada de visualização)
- **PostgreSQL** (banco de dados)
-  **Flyway** (migrações automáticas)
-  **Bootstrap 5 / Montserrat** (interface responsiva)
- **Maven** (gerenciador de dependências)

---

## Funcionalidades Principais

- Cadastro e gerenciamento de visitas  
- Filtro por loja e data  
- Login com controle de acesso (admin, gestor, compras, conferência)  
- Campos dinâmicos de observação e status  
- Exportação de relatórios em **PDF e XLSX**  
- Painel administrativo com filtros inteligentes  
- Migração automática de tabelas com Flyway  
- Landing page pública integrada ao sistema  

---

## 🧭 Estrutura do Projeto

SistemaDeVisitas/
├── src/
│ ├── main/
│ │ ├── java/com/inter/SistemaDeVisitas/
│ │ │ ├── controller/
│ │ │ ├── entity/
│ │ │ ├── repo/
│ │ │ ├── service/
│ │ │ └── config/
│ │ └── resources/
│ │ ├── static/
│ │ ├── templates/
│ │ └── db/migration/
│ └── test/
├── pom.xml
└── README.md



---

## ⚙️ Como Executar Localmente

1. **Clonar o repositório**
   ```bash
   git clone https://github.com/seuusuario/sistema-de-visitas.git
   cd sistema-de-visitas

   Configurar o banco de dados PostgreSQL

CREATE DATABASE sistema_visitas;


Editar o arquivo application.properties

spring.datasource.url=jdbc:postgresql://localhost:5432/sistema_visitas
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true

Executar o projeto

mvn spring-boot:run


Acesse: http://localhost:8080


🔐 Usuários e Permissões
Função	Permissões
Admin	Cadastra usuários, lojas e gerencia visitas, Conferência	Visualiza relatórios e exporta dados
loja	Registra visitas e adiciona observações, Conferência	Visualiza relatórios e exporta dados

🛠️ Próximas Atualizações

Integração com API de relatórios BI

Dashboard interativo com gráficos

Módulo mobile com registro via QR Code

Painel de indicadores por gestor

✨ Créditos

Desenvolvido e mantido por
Turma ADS 1 – | Tecnologia de Varejo
📍 Pernambuco / Alagoas
📧 victormanuelgama21@gmail.com

