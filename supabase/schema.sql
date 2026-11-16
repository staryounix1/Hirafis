-- ============================================================================
-- Hirafi (حرفي) - Supabase PostgreSQL Schema & Row Level Security (RLS)
-- Run this SQL in your Supabase Dashboard -> SQL Editor
-- ============================================================================

create extension if not exists "pgcrypto";

-- 1. Profiles (Clients & Craftsmen)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  phone text not null default '',
  city text not null default 'الدار البيضاء',
  role text not null default 'client' check (role in ('client', 'craftsman')),
  specialty text not null default 'سباكة',
  rating numeric(3,2) not null default 5.00,
  completed_jobs integer not null default 0,
  is_online boolean not null default false,
  is_verified boolean not null default false,
  lat double precision not null default 33.5731,
  lng double precision not null default -7.5898,
  updated_at timestamptz not null default now()
);

-- 2. Service Requests (inDrive-style job requests with client proposed fare)
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
  photo_uri text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 3. Service Offers (Live Craftsman Bids / Counter-Offers)
create table if not exists public.service_offers (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.service_requests(id) on delete cascade,
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
  created_at timestamptz not null default now()
);

-- 4. Chat Messages (Client <-> Craftsman negotiation & arrival coordination)
create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.service_requests(id) on delete cascade,
  sender_id text not null,
  sender_name text not null,
  sender_role text not null check (sender_role in ('client', 'craftsman')),
  message_text text not null,
  created_at timestamptz not null default now()
);

-- 5. Reviews
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
alter table public.profiles enable row level security;
alter table public.service_requests enable row level security;
alter table public.service_offers enable row level security;
alter table public.chat_messages enable row level security;
alter table public.reviews enable row level security;

-- Basic authenticated policies
create policy "Public read profiles" on public.profiles for select using (true);
create policy "Users manage own profile" on public.profiles for all using (auth.uid() = id) with check (auth.uid() = id);

create policy "Authenticated read requests" on public.service_requests for select using (auth.role() = 'authenticated');
create policy "Authenticated insert requests" on public.service_requests for insert with check (auth.uid() = client_id);
create policy "Participants update requests" on public.service_requests for update using (
  auth.uid() = client_id or auth.uid() = assigned_craftsman_id or status = 'searching'
);

create policy "Authenticated read offers" on public.service_offers for select using (auth.role() = 'authenticated');
create policy "Craftsmen insert offers" on public.service_offers for insert with check (auth.uid() = craftsman_id);
create policy "Participants update offers" on public.service_offers for update using (auth.role() = 'authenticated');

create policy "Participants read chat" on public.chat_messages for select using (auth.role() = 'authenticated');
create policy "Participants insert chat" on public.chat_messages for insert with check (auth.role() = 'authenticated');

create policy "Authenticated read reviews" on public.reviews for select using (true);
create policy "Clients insert reviews" on public.reviews for insert with check (auth.uid() = client_id);
