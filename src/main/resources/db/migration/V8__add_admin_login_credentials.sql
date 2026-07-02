alter table user_accounts
add column if not exists password_hash varchar(255);

insert into roles (
    code,
    name,
    active,
    created_at
)
select
    'ADMIN',
    'Administrador',
    true,
    now()
where not exists (
    select 1
    from roles
    where code = 'ADMIN'
);

insert into user_accounts (
    role_id,
    firebase_uid,
    email,
    first_name,
    last_name,
    dni,
    mobile_phone,
    landline_phone,
    address_text,
    address_reference,
    latitude,
    longitude,
    active,
    password_hash,
    created_at,
    updated_at
)
select
    r.id,
    null,
    'admin@servmedic.local',
    'Administrador',
    'ServMedic',
    '00000000',
    '900000000',
    null,
    'Oficina administrativa ServMedic',
    'Usuario administrador inicial',
    -12.0464000,
    -77.0428000,
    true,
    null,
    now(),
    now()
from roles r
where r.code = 'ADMIN'
and not exists (
    select 1
    from user_accounts ua
    where lower(ua.email) = lower('admin@servmedic.local')
);