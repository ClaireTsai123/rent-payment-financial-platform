create index ix_money_movements_created_at
    on money_movements (created_at desc);

create index ix_money_movements_type_created_at
    on money_movements (type, created_at desc);

create index ix_provider_transactions_status_created_at
    on provider_transactions (normalized_status, created_at desc);

create index ix_provider_transactions_provider_created_at
    on provider_transactions (provider, created_at desc);

create index ix_provider_webhook_events_normalized_status_received_at
    on provider_webhook_events (normalized_status, received_at desc);

create index ix_outbox_events_aggregate
    on outbox_events (aggregate_type, aggregate_id);

create index ix_settlement_records_provider_reference
    on settlement_records (provider, provider_transaction_reference);

create index ix_settlement_records_created_at
    on settlement_records (created_at desc);

create index ix_reconciliation_runs_started_at
    on reconciliation_runs (started_at desc);

create index ix_reconciliation_exceptions_provider_reference
    on reconciliation_exceptions (provider, provider_transaction_reference);

create index ix_reconciliation_exceptions_type_created_at
    on reconciliation_exceptions (exception_type, created_at desc);
