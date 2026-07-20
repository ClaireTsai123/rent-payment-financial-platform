create table settlement_records (
    id uuid primary key,
    money_movement_id uuid not null,
    provider_transaction_id uuid not null,
    status varchar(30) not null,
    expected_gross_amount numeric(19, 2) not null,
    expected_fee_amount numeric(19, 2) not null,
    expected_net_amount numeric(19, 2) not null,
    actual_gross_amount numeric(19, 2),
    actual_fee_amount numeric(19, 2),
    actual_net_amount numeric(19, 2),
    currency varchar(3) not null,
    expected_settlement_date date not null,
    actual_settlement_date date,
    provider varchar(80) not null,
    provider_transaction_reference varchar(160) not null,
    provider_batch_reference varchar(160),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_settlement_records_money_movement
        foreign key (money_movement_id) references money_movements (id),
    constraint fk_settlement_records_provider_transaction
        foreign key (provider_transaction_id) references provider_transactions (id),
    constraint uk_settlement_records_money_movement unique (money_movement_id),
    constraint uk_settlement_records_provider_transaction unique (provider_transaction_id)
);

create index ix_settlement_records_status on settlement_records (status);
create index ix_settlement_records_expected_date on settlement_records (expected_settlement_date);
