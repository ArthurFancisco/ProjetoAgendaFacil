create table establishments (
    id uuid primary key,
    name varchar(120) not null,
    slug varchar(120) not null unique,
    description varchar(500),
    phone varchar(30),
    city varchar(80),
    address varchar(180),
    instagram varchar(120),
    logo_url varchar(500),
    active boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp
);

create table app_users (
    id uuid primary key,
    name varchar(120) not null,
    email varchar(160) not null unique,
    password_hash varchar(255) not null,
    role varchar(30) not null,
    active boolean not null default true,
    establishment_id uuid references establishments(id),
    failed_attempts int not null default 0,
    locked_until timestamp,
    created_at timestamp not null,
    updated_at timestamp
);

create table clients (
    id uuid primary key,
    name varchar(120) not null,
    phone_original varchar(40) not null,
    phone_normalized varchar(20) not null,
    phone_verified boolean not null default false,
    status varchar(30) not null,
    no_show_count int not null default 0,
    establishment_id uuid not null references establishments(id),
    created_at timestamp not null,
    updated_at timestamp,
    constraint uk_client_establishment_phone unique(establishment_id, phone_normalized)
);

create table professionals (
    id uuid primary key,
    name varchar(120) not null,
    phone varchar(40),
    photo_url varchar(500),
    active boolean not null default true,
    establishment_id uuid not null references establishments(id),
    created_at timestamp not null,
    updated_at timestamp
);

create table services (
    id uuid primary key,
    name varchar(120) not null,
    description varchar(500),
    price numeric(10,2) not null,
    duration_minutes int not null,
    buffer_minutes int not null default 0,
    requires_manual_approval boolean not null default false,
    active boolean not null default true,
    establishment_id uuid not null references establishments(id),
    created_at timestamp not null,
    updated_at timestamp,
    constraint ck_service_duration check (duration_minutes > 0),
    constraint ck_service_buffer check (buffer_minutes >= 0),
    constraint ck_service_price check (price >= 0)
);

create table professional_services (
    professional_id uuid not null references professionals(id) on delete cascade,
    service_id uuid not null references services(id) on delete cascade,
    primary key (professional_id, service_id)
);

create table business_hours (
    id uuid primary key,
    establishment_id uuid not null references establishments(id),
    professional_id uuid references professionals(id),
    day_of_week int not null,
    open_time time not null,
    close_time time not null,
    active boolean not null default true,
    constraint ck_business_day check (day_of_week between 1 and 7),
    constraint ck_business_range check (open_time < close_time)
);

create table blocked_times (
    id uuid primary key,
    establishment_id uuid not null references establishments(id),
    professional_id uuid references professionals(id),
    block_date date not null,
    start_time time not null,
    end_time time not null,
    reason varchar(180) not null,
    created_by uuid references app_users(id),
    created_at timestamp not null,
    constraint ck_block_range check (start_time < end_time)
);

create table appointments (
    id uuid primary key,
    establishment_id uuid not null references establishments(id),
    client_id uuid not null references clients(id),
    professional_id uuid not null references professionals(id),
    service_id uuid not null references services(id),
    appointment_date date not null,
    start_time time not null,
    end_time time not null,
    status varchar(35) not null,
    expected_value numeric(10,2) not null,
    observation varchar(255),
    confirm_token varchar(80) not null unique,
    cancel_token varchar(80) not null unique,
    expires_at timestamp,
    confirmed_at timestamp,
    cancelled_at timestamp,
    cancellation_reason varchar(255),
    created_at timestamp not null,
    updated_at timestamp,
    version bigint not null default 0,
    constraint ck_appointment_range check (start_time < end_time)
);

create table schedule_attempts (
    id uuid primary key,
    establishment_id uuid not null,
    phone_normalized varchar(20),
    ip_hash varchar(100),
    user_agent_hash varchar(100),
    result varchar(40) not null,
    reason varchar(180),
    created_at timestamp not null
);

create table audit_logs (
    id uuid primary key,
    user_id uuid references app_users(id),
    establishment_id uuid references establishments(id),
    action varchar(80) not null,
    entity varchar(80) not null,
    entity_id varchar(80),
    ip_hash varchar(100),
    details varchar(500),
    created_at timestamp not null
);

create index idx_appointments_professional_date on appointments(professional_id, appointment_date);
create index idx_appointments_establishment_date on appointments(establishment_id, appointment_date);
create index idx_appointments_status_expires on appointments(status, expires_at);
create index idx_clients_establishment_status on clients(establishment_id, status);
create index idx_attempts_phone_time on schedule_attempts(establishment_id, phone_normalized, created_at);
create index idx_attempts_ip_time on schedule_attempts(establishment_id, ip_hash, created_at);
create index idx_blocked_times_lookup on blocked_times(establishment_id, professional_id, block_date);
