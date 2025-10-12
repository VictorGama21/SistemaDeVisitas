-- STORES (lojas)
create table if not exists stores (
  id bigserial primary key,
  name varchar(120) not null,
  cnpj varchar(32),
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create index if not exists idx_stores_active on stores(active);

-- liga usuário -> loja (1 loja por usuário, alterável depois)
alter table users
  add column if not exists store_id bigint null references stores(id);

create index if not exists idx_users_store on users(store_id);

-- VISITS
create type visit_status as enum ('PENDING','COMPLETED','NO_SHOW','REOPENED');

create table if not exists visits (
  id bigserial primary key,
  store_id bigint not null references stores(id),
  scheduled_at timestamptz not null,
  status visit_status not null default 'PENDING',
  comment text,
  rating integer check (rating between 1 and 5),
  created_by_user_id bigint not null references users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_visits_store on visits(store_id);
create index if not exists idx_visits_status on visits(status);
