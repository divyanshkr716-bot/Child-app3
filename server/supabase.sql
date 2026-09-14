-- Game Centre cloud schema. Run this once in Supabase SQL Editor.
create table if not exists public.parents (
  id text primary key,
  parent_token text not null unique,
  pairing_code text not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.devices (
  id text primary key,
  parent_id text not null references public.parents(id) on delete cascade,
  child_token text not null,
  name text not null default 'Child Device',
  model text not null default 'Android',
  os text not null default 'Android',
  last_seen timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists devices_parent_id_idx on public.devices(parent_id);
create index if not exists devices_last_seen_idx on public.devices(last_seen);
create index if not exists parents_pairing_code_idx on public.parents(pairing_code);

-- The Android apps never receive the service-role key. The Node relay is the only
-- component that talks to these tables with the service-role key.
alter table public.parents enable row level security;
alter table public.devices enable row level security;

-- Migration for older Game Centre versions: pairing codes do not expire.
alter table public.parents drop column if exists pairing_expires_at;
