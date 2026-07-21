create table reconciliation_runs (
    id uuid primary key,
    source_file varchar(500) not null,
    status varchar(30) not null,
    total_rows integer not null,
    matched_rows integer not null,
    exception_rows integer not null,
    started_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    failure_reason varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_reconciliation_runs_source_file unique (source_file)
);

create table reconciliation_exceptions (
    id uuid primary key,
    reconciliation_run_id uuid not null,
    exception_type varchar(40) not null,
    provider varchar(80) not null,
    provider_transaction_reference varchar(160) not null,
    message varchar(1000) not null,
    raw_record text not null,
    created_at timestamp with time zone not null,
    constraint fk_reconciliation_exceptions_run
        foreign key (reconciliation_run_id) references reconciliation_runs (id),
    constraint uk_reconciliation_exceptions_run_provider_reference_type
        unique (reconciliation_run_id, provider, provider_transaction_reference, exception_type)
);

create index ix_reconciliation_runs_status on reconciliation_runs (status);
create index ix_reconciliation_exceptions_run on reconciliation_exceptions (reconciliation_run_id);
