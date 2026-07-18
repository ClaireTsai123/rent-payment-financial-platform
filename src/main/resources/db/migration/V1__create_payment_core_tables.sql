create table payment_plans (
    id uuid primary key,
    renter_id varchar(80) not null,
    billing_obligation_id varchar(100) not null,
    rent_amount numeric(19, 2) not null,
    initial_collection_amount numeric(19, 2) not null,
    repayment_amount numeric(19, 2) not null,
    rent_due_date date not null,
    repayment_due_date date not null,
    status varchar(30) not null,
    version bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_payment_plans_billing_obligation unique (billing_obligation_id)
);

create table money_movements (
    id uuid primary key,
    payment_plan_id uuid not null,
    type varchar(40) not null,
    state varchar(30) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    operation_key varchar(160) not null,
    version bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_money_movements_payment_plan
        foreign key (payment_plan_id) references payment_plans (id),
    constraint uk_money_movements_operation_key unique (operation_key)
);

create index ix_money_movements_payment_plan on money_movements (payment_plan_id);
create index ix_money_movements_state on money_movements (state);

create table payment_attempts (
    id uuid primary key,
    money_movement_id uuid not null,
    attempt_number integer not null,
    status varchar(30) not null,
    failure_code varchar(80),
    failure_message varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_attempts_money_movement
        foreign key (money_movement_id) references money_movements (id),
    constraint uk_payment_attempts_movement_attempt unique (money_movement_id, attempt_number)
);

create index ix_payment_attempts_money_movement on payment_attempts (money_movement_id);

create table provider_transactions (
    id uuid primary key,
    money_movement_id uuid not null,
    payment_attempt_id uuid not null,
    provider varchar(80) not null,
    provider_transaction_id varchar(160) not null,
    provider_idempotency_key varchar(160),
    normalized_status varchar(30) not null,
    raw_status varchar(120),
    settlement_reference varchar(160),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_provider_transactions_money_movement
        foreign key (money_movement_id) references money_movements (id),
    constraint fk_provider_transactions_payment_attempt
        foreign key (payment_attempt_id) references payment_attempts (id),
    constraint uk_provider_transactions_provider_reference unique (provider, provider_transaction_id),
    constraint uk_provider_transactions_provider_idempotency unique (provider, provider_idempotency_key)
);

create index ix_provider_transactions_money_movement on provider_transactions (money_movement_id);
create index ix_provider_transactions_payment_attempt on provider_transactions (payment_attempt_id);

create table money_movement_state_history (
    id uuid primary key,
    money_movement_id uuid not null,
    from_state varchar(30),
    to_state varchar(30) not null,
    reason varchar(80) not null,
    changed_at timestamp with time zone not null,
    constraint fk_money_movement_state_history_money_movement
        foreign key (money_movement_id) references money_movements (id)
);

create index ix_money_movement_state_history_movement on money_movement_state_history (money_movement_id);

create table idempotency_records (
    id uuid primary key,
    idempotency_key varchar(160) not null,
    operation varchar(80) not null,
    request_fingerprint varchar(128) not null,
    status varchar(30) not null,
    resource_id uuid,
    response_payload text,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_idempotency_records_key_operation unique (idempotency_key, operation)
);

create index ix_idempotency_records_resource on idempotency_records (resource_id);

create table outbox_events (
    id uuid primary key,
    aggregate_type varchar(80) not null,
    aggregate_id uuid not null,
    event_type varchar(120) not null,
    payload text not null,
    status varchar(30) not null,
    attempts integer not null,
    last_error varchar(1000),
    published_at timestamp with time zone,
    next_attempt_at timestamp with time zone not null,
    version bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index ix_outbox_events_status_next_attempt_at on outbox_events (status, next_attempt_at);
create index ix_outbox_events_status_created_at on outbox_events (status, created_at);
