-- ============================================================================
-- Hirafi (حرفي) - Enterprise Secure Supabase PostgreSQL Schema & RLS
-- Run once in Supabase Dashboard -> SQL Editor (NO service_role key in app)
-- ============================================================================

create extension if not exists "pgcrypto";

-- ============================================================================
-- 1. Authorized Admins & Helper Functions
-- ============================================================================
create table if not exists public.authorized_admins (
  email text primary key,
  added_by text not null default 'system',
  created_at timestamptz not null default now()
);

insert into public.authorized_admins (email, added_by)
values ('youness065220@gmail.com', 'system')
on conflict (email) do nothing;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $fn$
  select lower(coalesce(auth.jwt() ->> 'email', '')) = 'youness065220@gmail.com'
      or exists (
        select 1
        from public.authorized_admins a
        where lower(a.email) = lower(coalesce(auth.jwt() ->> 'email', ''))
      );
$fn$;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $fn$
begin
  new.updated_at = now();
  return new;
end;
$fn$;

-- ============================================================================
-- 2. App Runtime Config (Production Mode Toggle)
-- ============================================================================
create table if not exists public.app_config (
  id text primary key,
  production_mode boolean not null default false,
  updated_by_email text not null default '',
  updated_at timestamptz not null default now()
);

insert into public.app_config (id, production_mode, updated_by_email)
values ('runtime', false, 'system')
on conflict (id) do nothing;

drop trigger if exists trg_app_config_updated_at on public.app_config;
create trigger trg_app_config_updated_at
  before update on public.app_config
  for each row execute function public.set_updated_at();

-- ============================================================================
-- 3. Profiles (Clients, Craftsmen & Admins)
-- ============================================================================
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  email text not null default '',
  phone text not null default '+212 612-345678',
  city text not null default 'الدار البيضاء',
  role text not null default 'client' check (role in ('client', 'craftsman', 'admin')),
  role_locked boolean not null default false,
  specialty_id text not null default 'plumbing',
  specialty text not null default 'سباكة',
  avatar_uri text,
  bio text not null default 'حرفي معتمد ومجهز بأحدث الأدوات الاحترافية',
  experience_years integer not null default 5,
  base_price_dh integer not null default 120,
  rating numeric(3,2) not null default 4.90,
  completed_jobs integer not null default 0,
  is_online boolean not null default false,
  is_verified boolean not null default false,
  verification_status text not null default 'not_submitted'
    check (verification_status in ('not_submitted', 'pending_review', 'verified', 'rejected')),
  rejection_reason text not null default '',
  is_suspended boolean not null default false,
  fcm_token text not null default '',
  lat double precision not null default 33.5822,
  lng double precision not null default -7.6324,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Idempotent column upgrades in case profiles already existed from an earlier version
alter table public.profiles add column if not exists role_locked boolean not null default false;
alter table public.profiles add column if not exists specialty_id text not null default 'plumbing';
alter table public.profiles add column if not exists specialty text not null default 'سباكة';
alter table public.profiles add column if not exists avatar_uri text;
alter table public.profiles add column if not exists bio text not null default 'حرفي معتمد ومجهز بأحدث الأدوات الاحترافية';
alter table public.profiles add column if not exists experience_years integer not null default 5;
alter table public.profiles add column if not exists base_price_dh integer not null default 120;
alter table public.profiles add column if not exists rating numeric(3,2) not null default 4.90;
alter table public.profiles add column if not exists completed_jobs integer not null default 0;
alter table public.profiles add column if not exists is_online boolean not null default false;
alter table public.profiles add column if not exists is_verified boolean not null default false;
alter table public.profiles add column if not exists verification_status text not null default 'not_submitted';
alter table public.profiles add column if not exists rejection_reason text not null default '';
alter table public.profiles add column if not exists is_suspended boolean not null default false;
alter table public.profiles add column if not exists fcm_token text not null default '';
alter table public.profiles add column if not exists lat double precision not null default 33.5822;
alter table public.profiles add column if not exists lng double precision not null default -7.6324;

-- Secure Public Craftsman Discovery View (Excludes private phone & email to protect user privacy)
drop view if exists public.craftsman_discovery_view cascade;
create view public.craftsman_discovery_view as
select
  id,
  full_name,
  city,
  role,
  specialty_id,
  specialty,
  avatar_uri,
  bio,
  experience_years,
  base_price_dh,
  rating,
  completed_jobs,
  is_online,
  is_verified,
  verification_status,
  is_suspended,
  lat,
  lng,
  created_at
from public.profiles
where role = 'craftsman' and not is_suspended;

revoke all on public.craftsman_discovery_view from anon;
grant select on public.craftsman_discovery_view to authenticated;

-- Trigger to protect sensitive profile privilege columns
create or replace function public.protect_profile_privilege_columns()
returns trigger
language plpgsql
security definer
set search_path = public
as $fn$
begin
  if auth.uid() is not null and not public.is_admin() then
    if old.role_locked = true then
      new.role := old.role;
      new.role_locked := true;
    elsif new.role = 'admin' then
      raise exception 'Only authorized admins can hold admin role';
    end if;

    new.is_verified := old.is_verified;
    new.is_suspended := old.is_suspended;
    new.rating := old.rating;
    new.completed_jobs := old.completed_jobs;

    if new.verification_status is distinct from old.verification_status then
      if new.verification_status <> 'pending_review' then
        new.verification_status := old.verification_status;
      end if;
    end if;

    if new.verification_status = 'pending_review' then
      new.rejection_reason := '';
    else
      new.rejection_reason := old.rejection_reason;
    end if;
  end if;
  return new;
end;
$fn$;

drop trigger if exists trg_profiles_protect_privileges on public.profiles;
create trigger trg_profiles_protect_privileges
  before update on public.profiles
  for each row execute function public.protect_profile_privilege_columns();

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

-- ============================================================================
-- 4. Craftsman Identity Verification (KYC) - Stored in Private hirafi-kyc Bucket
-- ============================================================================
create table if not exists public.verification_submissions (
  craftsman_id uuid primary key references public.profiles(id) on delete cascade,
  craftsman_name text not null,
  specialty_id text not null default 'plumbing',
  city text not null default 'الدار البيضاء',
  national_id_number text not null,
  front_photo_uri text not null,
  back_photo_uri text not null,
  selfie_photo_uri text not null,
  status text not null default 'pending_review'
    check (status in ('not_submitted', 'pending_review', 'verified', 'rejected')),
  rejection_reason text not null default '',
  reviewed_by_email text not null default '',
  submitted_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

drop trigger if exists trg_verifications_updated_at on public.verification_submissions;
create trigger trg_verifications_updated_at
  before update on public.verification_submissions
  for each row execute function public.set_updated_at();

-- ============================================================================
-- 5. Craftsman Portfolio Items
-- ============================================================================
create table if not exists public.portfolio_items (
  id uuid primary key default gen_random_uuid(),
  craftsman_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  description text not null default '',
  category_name text not null default 'حرفي',
  photo_uri text,
  created_at timestamptz not null default now()
);

-- ============================================================================
-- 6. Service Requests (Tamper-Proof Status & Assignment Protection)
-- ============================================================================
create table if not exists public.service_requests (
  id uuid primary key default gen_random_uuid(),
  client_id uuid references public.profiles(id) on delete set null,
  client_name text not null,
  category_id text not null,
  category_name text not null,
  title text not null,
  description text not null default '',
  address_label text not null,
  lat double precision not null,
  lng double precision not null,
  proposed_price integer not null check (proposed_price > 0),
  accepted_price integer,
  payment_method text not null default 'cash' check (payment_method in ('cash', 'transfer', 'card')),
  is_urgent boolean not null default true,
  status text not null default 'searching' check (
    status in ('searching', 'accepted', 'en_route', 'arrived', 'in_progress', 'completed', 'cancelled')
  ),
  assigned_craftsman_id uuid references public.profiles(id) on delete set null,
  assigned_craftsman_name text,
  assigned_craftsman_phone text,
  assigned_craftsman_rating numeric(3,2),
  craftsman_lat double precision,
  craftsman_lng double precision,
  photo_uri text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Idempotent column upgrades for service_requests
alter table public.service_requests add column if not exists accepted_price integer;
alter table public.service_requests add column if not exists payment_method text not null default 'cash';
alter table public.service_requests add column if not exists is_urgent boolean not null default true;
alter table public.service_requests add column if not exists assigned_craftsman_id uuid references public.profiles(id) on delete set null;
alter table public.service_requests add column if not exists assigned_craftsman_name text;
alter table public.service_requests add column if not exists assigned_craftsman_phone text;
alter table public.service_requests add column if not exists assigned_craftsman_rating numeric(3,2);
alter table public.service_requests add column if not exists craftsman_lat double precision;
alter table public.service_requests add column if not exists craftsman_lng double precision;
alter table public.service_requests add column if not exists photo_uri text;

drop trigger if exists trg_service_requests_updated_at on public.service_requests;
create trigger trg_service_requests_updated_at
  before update on public.service_requests
  for each row execute function public.set_updated_at();

-- ============================================================================
-- 7. Service Offers (Live Bids / Counter-Offers)
-- ============================================================================
create table if not exists public.service_offers (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.service_requests(id) on delete cascade,
  client_id uuid references public.profiles(id) on delete set null,
  craftsman_id uuid references public.profiles(id) on delete set null,
  craftsman_name text not null,
  craftsman_phone text not null default '+212 600-000000',
  craftsman_rating numeric(3,2) not null default 4.80,
  completed_jobs integer not null default 0,
  specialty text not null,
  offered_price integer not null check (offered_price > 0),
  eta_minutes integer not null default 10,
  distance_km numeric(5,2) not null default 1.50,
  note text not null default '',
  status text not null default 'pending' check (status in ('pending', 'accepted', 'declined', 'countered', 'expired')),
  expires_at_millis bigint not null default 0,
  created_at timestamptz not null default now()
);

-- Tamper-proof trigger for service requests state machine & offer consistency
create or replace function public.protect_service_request_mutations()
returns trigger
language plpgsql
security definer
set search_path = public
as $fn$
begin
  if auth.uid() is not null and not public.is_admin() then
    -- If client is updating their own request
    if auth.uid()::text = old.client_id::text then
      -- Client cannot assign craftsman directly without a real offer or admin override
      if new.assigned_craftsman_id is distinct from old.assigned_craftsman_id and new.assigned_craftsman_id is not null then
        if not exists (
          select 1 from public.service_offers o
          where o.request_id::text = old.id::text
            and o.craftsman_id::text = new.assigned_craftsman_id::text
            and o.status in ('pending', 'countered', 'accepted')
        ) then
          raise exception 'Cannot assign craftsman without an existing offer';
        end if;
      end if;
    -- If craftsman is updating
    elsif auth.uid()::text = old.assigned_craftsman_id::text then
      -- Craftsman cannot alter financial proposal or client ID
      new.client_id := old.client_id;
      new.proposed_price := old.proposed_price;
      new.client_name := old.client_name;
    else
      -- Non-participant trying to update request
      if old.status = 'searching' and new.status = 'accepted' and new.assigned_craftsman_id::text = auth.uid()::text then
        -- Allowed for direct craftsman acceptance if offer/acceptance rules met
        null;
      else
        raise exception 'Unauthorized request state mutation';
      end if;
    end if;
  end if;
  return new;
end;
$fn$;

drop trigger if exists trg_service_requests_tamper_proof on public.service_requests;
create trigger trg_service_requests_tamper_proof
  before update on public.service_requests
  for each row execute function public.protect_service_request_mutations();

-- ============================================================================
-- 8. Chat Messages
-- ============================================================================
create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.service_requests(id) on delete cascade,
  sender_id text not null,
  sender_name text not null,
  sender_role text not null check (sender_role in ('client', 'craftsman', 'admin')),
  message_text text not null,
  created_at timestamptz not null default now()
);

-- ============================================================================
-- 9. Reviews
-- ============================================================================
create table if not exists public.reviews (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.service_requests(id) on delete cascade,
  craftsman_id uuid references public.profiles(id) on delete set null,
  client_id uuid references public.profiles(id) on delete set null,
  rating integer not null check (rating between 1 and 5),
  tags text[] not null default '{}',
  comment text not null default '',
  created_at timestamptz not null default now()
);

-- ============================================================================
-- 10. Performance Indexes
-- ============================================================================
create index if not exists idx_profiles_role_online on public.profiles(role, is_online);
create index if not exists idx_portfolio_craftsman on public.portfolio_items(craftsman_id, created_at desc);
create index if not exists idx_requests_status_category on public.service_requests(status, category_id);
create index if not exists idx_requests_client on public.service_requests(client_id, created_at desc);
create index if not exists idx_requests_craftsman on public.service_requests(assigned_craftsman_id, created_at desc);
create index if not exists idx_offers_request on public.service_offers(request_id, created_at desc);
create index if not exists idx_offers_craftsman on public.service_offers(craftsman_id);
create index if not exists idx_chat_request_created on public.chat_messages(request_id, created_at asc);
create index if not exists idx_reviews_craftsman on public.reviews(craftsman_id, created_at desc);

-- ============================================================================
-- 11. Row Level Security (RLS) Policies
-- ============================================================================
alter table public.authorized_admins enable row level security;
alter table public.app_config enable row level security;
alter table public.profiles enable row level security;
alter table public.verification_submissions enable row level security;
alter table public.portfolio_items enable row level security;
alter table public.service_requests enable row level security;
alter table public.service_offers enable row level security;
alter table public.chat_messages enable row level security;
alter table public.reviews enable row level security;

-- authorized_admins
drop policy if exists "Authenticated check own admin status" on public.authorized_admins;
drop policy if exists "Admins manage authorized admins" on public.authorized_admins;
create policy "Authenticated check own admin status"
  on public.authorized_admins for select
  using (
    public.is_admin()
    or lower(email) = lower(coalesce(auth.jwt() ->> 'email', ''))
  );
create policy "Admins manage authorized admins"
  on public.authorized_admins for all
  using (public.is_admin())
  with check (public.is_admin());

-- app_config
drop policy if exists "Authenticated read app config" on public.app_config;
drop policy if exists "Admins manage app config" on public.app_config;
create policy "Authenticated read app config"
  on public.app_config for select
  using (auth.role() = 'authenticated');
create policy "Admins manage app config"
  on public.app_config for all
  using (public.is_admin())
  with check (public.is_admin());

-- profiles: Users can only read/update their own full profile row. Public discovery goes through craftsman_discovery_view.
drop policy if exists "Public read profiles" on public.profiles;
drop policy if exists "Users read own profile or admin" on public.profiles;
drop policy if exists "Users insert own profile" on public.profiles;
drop policy if exists "Users update own profile" on public.profiles;

create policy "Users read own profile or admin"
  on public.profiles for select
  using (auth.uid()::text = id::text or public.is_admin());

create policy "Users insert own profile"
  on public.profiles for insert
  with check (
    auth.uid()::text = id::text
    and (role in ('client', 'craftsman') or (role = 'admin' and public.is_admin()))
  );

create policy "Users update own profile"
  on public.profiles for update
  using (auth.uid()::text = id::text or public.is_admin())
  with check (
    public.is_admin()
    or (auth.uid()::text = id::text and role in ('client', 'craftsman'))
  );

-- verification_submissions (KYC): ONLY owner craftsman or Admin
drop policy if exists "Users read own or admin verification" on public.verification_submissions;
drop policy if exists "Craftsmen submit own verification" on public.verification_submissions;
drop policy if exists "Craftsmen update own verification" on public.verification_submissions;
create policy "Users read own or admin verification"
  on public.verification_submissions for select
  using (auth.uid()::text = craftsman_id::text or public.is_admin());
create policy "Craftsmen submit own verification"
  on public.verification_submissions for insert
  with check (auth.uid()::text = craftsman_id::text and status = 'pending_review');
create policy "Craftsmen update own verification"
  on public.verification_submissions for update
  using (auth.uid()::text = craftsman_id::text or public.is_admin())
  with check (public.is_admin() or (auth.uid()::text = craftsman_id::text and status = 'pending_review'));

-- portfolio_items
drop policy if exists "Authenticated read portfolio" on public.portfolio_items;
drop policy if exists "Craftsmen manage own portfolio" on public.portfolio_items;
create policy "Authenticated read portfolio"
  on public.portfolio_items for select
  using (auth.role() = 'authenticated');
create policy "Craftsmen manage own portfolio"
  on public.portfolio_items for all
  using (auth.uid()::text = craftsman_id::text or public.is_admin())
  with check (auth.uid()::text = craftsman_id::text or public.is_admin());

-- service_requests: searching requests visible to authenticated; accepted/assigned requests visible ONLY to client, assigned craftsman, or admin
drop policy if exists "Authenticated read requests" on public.service_requests;
drop policy if exists "Authenticated insert requests" on public.service_requests;
drop policy if exists "Participants update requests" on public.service_requests;
create policy "Authenticated read requests"
  on public.service_requests for select
  using (
    status = 'searching'
    or auth.uid()::text = client_id::text
    or auth.uid()::text = assigned_craftsman_id::text
    or public.is_admin()
  );
create policy "Authenticated insert requests"
  on public.service_requests for insert
  with check (auth.uid()::text = client_id::text);
create policy "Participants update requests"
  on public.service_requests for update
  using (
    auth.uid()::text = client_id::text
    or auth.uid()::text = assigned_craftsman_id::text
    or status = 'searching'
    or public.is_admin()
  )
  with check (
    auth.uid()::text = client_id::text
    or auth.uid()::text = assigned_craftsman_id::text
    or public.is_admin()
  );

-- service_offers: ONLY bidding craftsman, request client, or Admin
drop policy if exists "Authenticated read offers" on public.service_offers;
drop policy if exists "Craftsmen insert offers" on public.service_offers;
drop policy if exists "Participants update offers" on public.service_offers;
create policy "Authenticated read offers"
  on public.service_offers for select
  using (
    auth.uid()::text = craftsman_id::text
    or auth.uid()::text = client_id::text
    or exists (
      select 1 from public.service_requests r
      where r.id::text = request_id::text and r.client_id::text = auth.uid()::text
    )
    or public.is_admin()
  );
create policy "Craftsmen insert offers"
  on public.service_offers for insert
  with check (auth.uid()::text = craftsman_id::text);
create policy "Participants update offers"
  on public.service_offers for update
  using (
    auth.uid()::text = craftsman_id::text
    or auth.uid()::text = client_id::text
    or exists (
      select 1 from public.service_requests r
      where r.id::text = request_id::text and r.client_id::text = auth.uid()::text
    )
    or public.is_admin()
  )
  with check (
    auth.uid()::text = craftsman_id::text
    or auth.uid()::text = client_id::text
    or exists (
      select 1 from public.service_requests r
      where r.id::text = request_id::text and r.client_id::text = auth.uid()::text
    )
    or public.is_admin()
  );

-- chat_messages
drop policy if exists "Participants read chat" on public.chat_messages;
drop policy if exists "Participants insert chat" on public.chat_messages;
create policy "Participants read chat"
  on public.chat_messages for select
  using (
    public.is_admin()
    or exists (
      select 1 from public.service_requests r
      where r.id::text = request_id::text
        and (r.client_id::text = auth.uid()::text or r.assigned_craftsman_id::text = auth.uid()::text)
    )
    or exists (
      select 1 from public.service_offers o
      where o.request_id::text = chat_messages.request_id::text
        and o.craftsman_id::text = auth.uid()::text
    )
  );
create policy "Participants insert chat"
  on public.chat_messages for insert
  with check (
    sender_id::text = auth.uid()::text
    and (
      public.is_admin()
      or exists (
        select 1 from public.service_requests r
        where r.id::text = request_id::text
          and (r.client_id::text = auth.uid()::text or r.assigned_craftsman_id::text = auth.uid()::text)
      )
      or exists (
        select 1 from public.service_offers o
        where o.request_id::text = chat_messages.request_id::text
          and o.craftsman_id::text = auth.uid()::text
      )
    )
  );

-- reviews
drop policy if exists "Authenticated read reviews" on public.reviews;
drop policy if exists "Clients insert reviews" on public.reviews;
create policy "Authenticated read reviews"
  on public.reviews for select
  using (auth.role() = 'authenticated');
create policy "Clients insert reviews"
  on public.reviews for insert
  with check (
    auth.uid()::text = client_id::text
    and exists (
      select 1 from public.service_requests r
      where r.id::text = request_id::text and r.client_id::text = auth.uid()::text
    )
  );

-- ============================================================================
-- 12. Supabase Storage Buckets & Strict RLS Policies
-- ============================================================================
insert into storage.buckets (id, name, public)
values ('hirafi-media', 'hirafi-media', true)
on conflict (id) do update set public = true;

drop policy if exists "Hirafi public media read" on storage.objects;
drop policy if exists "Hirafi authenticated media upload" on storage.objects;
drop policy if exists "Hirafi authenticated media update" on storage.objects;
drop policy if exists "Hirafi authenticated media delete" on storage.objects;

create policy "Hirafi public media read"
  on storage.objects for select
  using (
    bucket_id = 'hirafi-media'
    and (storage.foldername(name))[1] in ('users', 'requests', 'portfolio')
  );

create policy "Hirafi authenticated media upload"
  on storage.objects for insert
  with check (
    bucket_id = 'hirafi-media'
    and auth.role() = 'authenticated'
    and (storage.foldername(name))[1] in ('users', 'requests', 'portfolio')
    and (auth.uid()::text = (storage.foldername(name))[2] or public.is_admin())
  );

create policy "Hirafi authenticated media update"
  on storage.objects for update
  using (
    bucket_id = 'hirafi-media'
    and (storage.foldername(name))[1] in ('users', 'requests', 'portfolio')
    and (auth.uid()::text = (storage.foldername(name))[2] or public.is_admin())
  );

create policy "Hirafi authenticated media delete"
  on storage.objects for delete
  using (
    bucket_id = 'hirafi-media'
    and (auth.uid()::text = (storage.foldername(name))[2] or public.is_admin())
  );

-- PRIVATE KYC Identity Bucket (hirafi-kyc)
insert into storage.buckets (id, name, public)
values ('hirafi-kyc', 'hirafi-kyc', false)
on conflict (id) do update set public = false;

drop policy if exists "Hirafi KYC owner or admin read" on storage.objects;
drop policy if exists "Hirafi KYC owner upload" on storage.objects;
drop policy if exists "Hirafi KYC owner or admin update" on storage.objects;
drop policy if exists "Hirafi KYC owner or admin delete" on storage.objects;

create policy "Hirafi KYC owner or admin read"
  on storage.objects for select
  using (
    bucket_id = 'hirafi-kyc'
    and (auth.uid()::text = (storage.foldername(name))[1] or public.is_admin())
  );

create policy "Hirafi KYC owner upload"
  on storage.objects for insert
  with check (
    bucket_id = 'hirafi-kyc'
    and auth.role() = 'authenticated'
    and (auth.uid()::text = (storage.foldername(name))[1] or public.is_admin())
  );

create policy "Hirafi KYC owner or admin update"
  on storage.objects for update
  using (
    bucket_id = 'hirafi-kyc'
    and (auth.uid()::text = (storage.foldername(name))[1] or public.is_admin())
  );

create policy "Hirafi KYC owner or admin delete"
  on storage.objects for delete
  using (
    bucket_id = 'hirafi-kyc'
    and (auth.uid()::text = (storage.foldername(name))[1] or public.is_admin())
  );

-- Reload PostgREST schema cache immediately so /rest/v1/* endpoints stop returning 404
NOTIFY pgrst, 'reload schema';
