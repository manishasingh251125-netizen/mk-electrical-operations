create or replace function public.sync_employee_verification()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.verification_code is null or length(trim(new.verification_code)) = 0 then
    new.verification_code := encode(gen_random_bytes(24), 'hex');
  end if;
  if new.id_serial is null or length(trim(new.id_serial)) = 0 then
    new.id_serial := 'MKE-ID-' || new.employee_code;
  end if;
  insert into public.employee_verifications(verification_code, employee_id, full_name, designation, is_active, updated_at)
  values (new.verification_code, new.employee_code, new.full_name, new.designation, new.is_active, now())
  on conflict (verification_code) do update
  set employee_id=excluded.employee_id, full_name=excluded.full_name,
      designation=excluded.designation, is_active=excluded.is_active, updated_at=now();
  return new;
end;
$$;
drop trigger if exists profiles_sync_employee_verification on public.profiles;
create trigger profiles_sync_employee_verification
before insert or update of employee_code, full_name, designation, is_active, verification_code, id_serial
on public.profiles for each row execute function public.sync_employee_verification();
update public.profiles
set verification_code=coalesce(nullif(verification_code,''), encode(gen_random_bytes(24),'hex')),
    id_serial=coalesce(nullif(id_serial,''), 'MKE-ID-' || employee_code);
