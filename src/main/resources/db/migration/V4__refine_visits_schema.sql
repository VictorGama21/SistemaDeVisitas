-- V4__refine_visits_schema.sql
-- Ajusta a estrutura de visitas para suportar múltiplas lojas, catálogos auxiliares
-- e os novos campos utilizados pelas entidades Visit/Buyer/Supplier/Segment.

-- Catálogos auxiliares ------------------------------------------------------
create table if not exists buyers (
  id bigserial primary key,
  name varchar(120) not null unique,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_buyers_active on buyers(active);

create table if not exists suppliers (
  id bigserial primary key,
  name varchar(120) not null unique,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_suppliers_active on suppliers(active);

create table if not exists segments (
  id bigserial primary key,
  name varchar(120) not null unique,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_segments_active on segments(active);

-- Estrutura de visitas ------------------------------------------------------
-- Garante coluna scheduled_date (LocalDate)
alter table visits
  add column if not exists scheduled_date date;

update visits
   set scheduled_date = coalesce(scheduled_date, scheduled_at::date, now()::date)
 where scheduled_date is null;

alter table visits
  alter column scheduled_date set not null;

create index if not exists idx_visits_scheduled_date on visits(scheduled_date);

-- Modalidade armazenada como texto (enum java)
alter table visits
  add column if not exists modality varchar(32);

update visits
   set modality = coalesce(nullif(trim(modality), ''), 'PROMOTORIA_REPOSICAO')
 where modality is null or trim(modality) = '';

alter table visits
  alter column modality set not null,
  alter column modality set default 'PROMOTORIA_REPOSICAO';

-- Campos adicionais
alter table visits add column if not exists commercial_info text;
alter table visits add column if not exists buyer_id bigint references buyers(id);
alter table visits add column if not exists supplier_id bigint references suppliers(id);
alter table visits add column if not exists segment_id bigint references segments(id);

alter table visits
  alter column created_at set default now(),
  alter column updated_at set default now();

create index if not exists idx_visits_buyer on visits(buyer_id);
create index if not exists idx_visits_supplier on visits(supplier_id);
create index if not exists idx_visits_segment on visits(segment_id);

-- Relacionamento visitas <-> lojas (N:N)
create table if not exists visit_stores (
  visit_id bigint not null references visits(id) on delete cascade,
  store_id bigint not null references stores(id),
  primary key (visit_id, store_id)
);

create index if not exists idx_visit_stores_store on visit_stores(store_id);

insert into visit_stores (visit_id, store_id)
select id, store_id
  from visits
 where store_id is not null
on conflict do nothing;

-- Limpa colunas legadas
alter table visits drop constraint if exists visits_store_id_fkey;
drop index if exists idx_visits_store;

alter table visits drop column if exists store_id;
alter table visits drop column if exists scheduled_at;
