create table train_version
(
    version         uuid                     not null,
    train_id        uuid                     not null,
    train_number    varchar(255)             not null,
    operational_day date                     not null,
    branch_name     varchar(255)             not null,
    owner           varchar(255)             not null,
    comment         text                     not null,
    timestamp       timestamp with time zone not null,
    payload         jsonb                    not null,
    branch_state    varchar(255)             not null,
    head            boolean                  not null,
    primary key (version)
);
create index idx_train_version_train_id on train_version (train_id);
create index idx_train_version_operational_day on train_version (operational_day);

create table train_partner
(
    train_id uuid         not null,
    partner  varchar(255) not null,
    primary key (train_id, partner)
);
create index idx_train_partner_train_id on train_partner (train_id);

create table changelog
(
    id          uuid                     not null,
    action      varchar(255)             not null,
    partner     varchar(255)             not null,
    train_id    uuid                     not null,
    branch_name varchar(255)             not null,
    version     uuid                     not null,
    timestamp   timestamp with time zone not null,
    comment     text                     not null,
    primary key (id)
);
create index idx_changelog_timestamp on changelog (timestamp);
create index idx_changelog_train_id on changelog (train_id);
