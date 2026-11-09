create table if not exists public.room_survey_entries (
  id uuid primary key default gen_random_uuid(),
  site_id uuid not null references public.sites(id) on delete cascade,
  room_name text not null check (length(trim(room_name)) between 1 and 120),
  item_description text not null check (length(trim(item_description)) between 1 and 500),
  quantity numeric(12,2) not null default 1 check (quantity > 0),
  unit text not null default 'Nos' check (length(trim(unit)) between 1 and 24),
  notes text,
  created_by uuid not null references public.profiles(id),
  created_at timestamptz not null default now()
);
create index if not exists room_survey_entries_site_created_idx on public.room_survey_entries(site_id, created_at desc);
alter table public.room_survey_entries enable row level security;
grant select, insert, update, delete on public.room_survey_entries to authenticated;
create policy room_survey_owner_manage on public.room_survey_entries for all to authenticated using ((auth.jwt()->'app_metadata'->>'role')='owner') with check ((auth.jwt()->'app_metadata'->>'role')='owner');
create policy room_survey_worker_read_assigned on public.room_survey_entries for select to authenticated using (exists (select 1 from public.site_assignments a where a.site_id=room_survey_entries.site_id and a.worker_id=(select auth.uid()) and a.is_active=true));
create policy room_survey_worker_insert_assigned on public.room_survey_entries for insert to authenticated with check (created_by=(select auth.uid()) and exists (select 1 from public.site_assignments a where a.site_id=room_survey_entries.site_id and a.worker_id=(select auth.uid()) and a.is_active=true));

create table if not exists public.tool_custody (
  id uuid primary key default gen_random_uuid(),
  tool_name text not null check (length(trim(tool_name)) between 1 and 120),
  serial_number text,
  assigned_to uuid not null references public.profiles(id),
  site_id uuid references public.sites(id) on delete set null,
  issued_at timestamptz not null default now(),
  returned_at timestamptz,
  condition_out text,
  condition_in text,
  notes text,
  created_by uuid not null references public.profiles(id),
  created_at timestamptz not null default now(),
  check (returned_at is null or returned_at >= issued_at)
);
create index if not exists tool_custody_assigned_open_idx on public.tool_custody(assigned_to, returned_at, issued_at desc);
alter table public.tool_custody enable row level security;
grant select, insert, update, delete on public.tool_custody to authenticated;
create policy tool_custody_owner_manage on public.tool_custody for all to authenticated using ((auth.jwt()->'app_metadata'->>'role')='owner') with check ((auth.jwt()->'app_metadata'->>'role')='owner');
create policy tool_custody_worker_read_own on public.tool_custody for select to authenticated using (assigned_to=(select auth.uid()));
