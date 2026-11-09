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
create index if not exists room_survey_entries_created_by_idx on public.room_survey_entries(created_by);
alter table public.room_survey_entries enable row level security;
grant select, insert, update, delete on public.room_survey_entries to authenticated;
create policy room_survey_read_assigned_or_owner on public.room_survey_entries for select to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='owner' or exists (select 1 from public.site_assignments a where a.site_id=room_survey_entries.site_id and a.worker_id=(select auth.uid()) and a.is_active=true));
create policy room_survey_insert_assigned_or_owner on public.room_survey_entries for insert to authenticated
with check (((select auth.jwt()->'app_metadata'->>'role')='owner') or (created_by=(select auth.uid()) and exists (select 1 from public.site_assignments a where a.site_id=room_survey_entries.site_id and a.worker_id=(select auth.uid()) and a.is_active=true)));
create policy room_survey_owner_update on public.room_survey_entries for update to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='owner') with check ((select auth.jwt()->'app_metadata'->>'role')='owner');
create policy room_survey_owner_delete on public.room_survey_entries for delete to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='owner');

alter table public.tool_custody enable row level security;
grant select, insert, update, delete on public.tool_custody to authenticated;
create policy tool_custody_read_own_or_owner on public.tool_custody for select to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='owner' or assigned_to=(select auth.uid()));
create policy tool_custody_owner_insert on public.tool_custody for insert to authenticated
with check ((select auth.jwt()->'app_metadata'->>'role')='owner');
create policy tool_custody_owner_update on public.tool_custody for update to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='owner') with check ((select auth.jwt()->'app_metadata'->>'role')='owner');
create policy tool_custody_owner_delete on public.tool_custody for delete to authenticated
using ((select auth.jwt()->'app_metadata'->>'role')='owner');
