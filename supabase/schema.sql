-- ============================================================================
-- Hirafi (حرفي) - Complete Supabase PostgreSQL Schema, RLS & Storage Policies
-- Run this entire script once in Supabase Dashboard -> SQL Editor
-- ============================================================================

create extension if not exists "pgcrypto";

-- 1. Authorized Admins Table
create table if not exists public.authorized_admins (
  email text primary key,
  added_by text not null default 'system',
  created_at timestamptz not null default now()
);

insert into public.authorized_admins (email, added_by)
values ('youness065220@gmail.com', 'system')
on conflict (email) do nothing;

-- Helper function to check Admin status safely without RLS recursion
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select lower(coalesce(auth.jwt() ->> 'email', '')) = 'youness065220@gmail.com'
      or exists (
        select 1
        from public.authorized_admins a
        where lower(a.email) = lower(coalesce(auth.jwt() ->> 'email', ''))
      );
$$;

-- 2. App Runtime Config (Production Mode toggle)
create table if not exists public.app_config (
  id text primary key,
  production_mode boolean not null default false,
  updated_by_email text not null default '',
  updated_at timestamptz not null default now()
);

insert into public.app_config (id, production_mode, updated_by_email)
values ('runtime', false, 'system')
on conflict (id) do nothing;

-- 3. Profiles (Clients, Craftsmen & Admins)
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

-- Upgrade existing profiles table safely if re-running
alter table public.profiles add column if not exists email text not null default '';
alter table public.profiles add column if not exists role_locked boolean not null default false;
alter table public.profiles add column if not exists specialty_id text not null default 'plumbing';
alter table public.profiles add column if not exists avatar_uri text;
alter table public.profiles add column if not exists bio text not null default 'حرفي معتمد ومجهز بأحدث الأدوات الاحترافية';
alter table public.profiles add column if not exists experience_years integer not null default 5;
alter table public.profiles add column if not exists base_price_dh integer not null default 120;
alter table public.profiles add column if not exists verification_status text not null default 'not_submitted';
alter table public.profiles add column if not exists rejection_reason text not null default '';
alter table public.profiles add column if not exists is_suspended boolean not null default false;
alter table public.profiles add column if not exists fcm_token text not null default '';
alter table public.profiles add column if not exists created_at timestamptz not null default now();
alter table public.profiles drop constraint if exists profiles_role_check;
alter table public.profiles add constraint profiles_role_check check (role in ('client', 'craftsman', 'admin'));

-- 4. Craftsman Identity Verification (KYC)
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

-- 5. Craftsman Portfolio Items
create table if not exists public.portfolio_items (
  id uuid primary key default gen_random_uuid(),
  craftsman_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  description text not null default '',
  category_name text not null default 'حرفي',
  photo_uri text,
  created_at timestamptz not null default now()
);

-- 6. Service Requests (inDrive-style job requests with client proposed fare)
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

alter table public.service_requests add column if not exists assigned_craftsman_rating numeric(3,2);
alter table public.service_requests add column if not exists craftsman_lat double precision;
alter table public.service_requests add column if not exists craftsman_lng double precision;

-- 7. Service Offers (Live Craftsman Bids / Counter-Offers)
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

alter table public.service_offers add column if not exists client_id uuid references public.profiles(id) on delete set null;
alter table public.service_offers add column if not exists expires_at_millis bigint not null default 0;

-- 8. Chat Messages
create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.service_requests(id) on delete cascade,
  sender_id text not null,
  sender_name text not null,
  sender_role text not null check (sender_role in ('client', 'craftsman', 'admin')),
  message_text text not null,
  created_at timestamptz not null default now()
);

-- 9. Reviews
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

-- Enable Row Level Security
alter table public.authorized_admins enable row level security;
alter table public.app_config enable row level security;
alter table public.profiles enable row level security;
alter table public.verification_submissions enable row level security;
alter table public.portfolio_items enable row level security;
alter table public.service_requests enable row level security;
alter table public.service_offers enable row level security;
alter table public.chat_messages enable row level security;
alter table public.reviews enable row level security;

-- Drop older policies before recreating cleanly
drop policy if exists "Admins manage authorized admins" on public.authorized_admins;
drop policy if exists "Authenticated read authorized admins" on public.authorized_admins;
create policy "Authenticated read authorized admins"
  on public.authorized_admins for select
  using (auth.role() = 'authenticated');
create policy "Admins manage authorized admins"
  on public.authorized_admins for all
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists "Authenticated read app config" on public.app_config;
drop policy if exists "Admins manage app config" on public.app_config;
create policy "Authenticated read app config"
  on public.app_config for select
  using (auth.role() = 'authenticated');
create policy "Admins manage app config"
  on public.app_config for all
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists "Public read profiles" on public.profiles;
drop policy if exists "Users manage own profile" on public.profiles;
drop policy if exists "Users insert own profile" on public.profiles;
drop policy if exists "Users update own profile" on public.profiles;
drop policy if exists "Admins update any profile" on public.profiles;

create policy "Public read profiles"
  on public.profiles for select
  using (auth.role() = 'authenticated');

create policy "Users insert own profile"
  on public.profiles for insert
  with check (
    auth.uid() = id
    and (role in ('client', 'craftsman') or (role = 'admin' and public.is_admin()))
  );

create policy "Users update own profile"
  on public.profiles for update
  using (auth.uid() = id or public.is_admin())
  with check (
    public.is_admin()
    or (
      auth.uid() = id
      and role in ('client', 'craftsman')
    )
  );

drop policy if exists "Users read own or admin verification" on public.verification_submissions;
drop policy if exists "Craftsmen submit own verification" on public.verification_submissions;
drop policy if exists "Admins review verification" on public.verification_submissions;
create policy "Users read own or admin verification"
  on public.verification_submissions for select
  using (auth.uid() = craftsman_id or public.is_admin());
create policy "Craftsmen submit own verification"
  on public.verification_submissions for insert
  with check (auth.uid() = craftsman_id and status = 'pending_review');
create policy "Craftsmen update own verification"
  on public.verification_submissions for update
  using (auth.uid() = craftsman_id or public.is_admin())
  with check (public.is_admin() or (auth.uid() = craftsman_id and status = 'pending_review'));

drop policy if exists "Authenticated read portfolio" on public.portfolio_items;
drop policy if exists "Craftsmen manage own portfolio" on public.portfolio_items;
create policy "Authenticated read portfolio"
  on public.portfolio_items for select
  using (auth.role() = 'authenticated');
create policy "Craftsmen manage own portfolio"
  on public.portfolio_items for all
  using (auth.uid() = craftsman_id or public.is_admin())
  with check (auth.uid() = craftsman_id or public.is_admin());

drop policy if exists "Authenticated read requests" on public.service_requests;
drop policy if exists "Authenticated insert requests" on public.service_requests;
drop policy if exists "Participants update requests" on public.service_requests;
create policy "Authenticated read requests"
  on public.service_requests for select
  using (auth.role() = 'authenticated');
create policy "Authenticated insert requests"
  on public.service_requests for insert
  with check (auth.uid() = client_id);
create policy "Participants update requests"
  on public.service_requests for update
  using (
    auth.uid() = client_id
    or auth.uid() = assigned_craftsman_id
    or status = 'searching'
    or public.is_admin()
  );

drop policy if exists "Authenticated read offers" on public.service_offers;
drop policy if exists "Craftsmen insert offers" on public.service_offers;
drop policy if exists "Participants update offers" on public.service_offers;
create policy "Authenticated read offers"
  on public.service_offers for select
  using (auth.role() = 'authenticated');
create policy "Craftsmen insert offers"
  on public.service_offers for insert
  with check (auth.uid() = craftsman_id);
create policy "Participants update offers"
  on public.service_offers for update
  using (auth.role() = 'authenticated');

drop policy if exists "Participants read chat" on public.chat_messages;
drop policy if exists "Participants insert chat" on public.chat_messages;
create policy "Participants read chat"
  on public.chat_messages for select
  using (auth.role() = 'authenticated');
create policy "Participants insert chat"
  on public.chat_messages for insert
  with check (auth.role() = 'authenticated');

drop policy if exists "Authenticated read reviews" on public.reviews;
drop policy if exists "Clients insert reviews" on public.reviews;
create policy "Authenticated read reviews"
  on public.reviews for select
  using (auth.role() = 'authenticated');
create policy "Clients insert reviews"
  on public.reviews for insert
  with check (auth.uid() = client_id);

-- ============================================================================
-- Supabase Storage Bucket & Policies (hirafi-media)
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
  using (bucket_id = 'hirafi-media');

create policy "Hirafi authenticated media upload"
  on storage.objects for insert
  with check (
    bucket_id = 'hirafi-media'
    and auth.role() = 'authenticated'
  );

create policy "Hirafi authenticated media update"
  on storage.objects for update
  using (
    bucket_id = 'hirafi-media'
    and auth.role() = 'authenticated'
  );

create policy "Hirafi authenticated media delete"
  on storage.objects for delete
  using (
    bucket_id = 'hirafi-media'
    and (auth.uid()::text = (storage.foldername(name))[2] or public.is_admin())
  );
