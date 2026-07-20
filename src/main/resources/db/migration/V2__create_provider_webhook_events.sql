create table provider_webhook_events (
    id uuid primary key,
    provider varchar(80) not null,
    provider_event_id varchar(160) not null,
    provider_transaction_id varchar(160) not null,
    normalized_status varchar(30) not null,
    raw_payload text not null,
    processing_status varchar(30) not null,
    failure_reason varchar(500),
    occurred_at timestamp with time zone not null,
    received_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    constraint uk_provider_webhook_events_provider_event unique (provider, provider_event_id)
);

create index ix_provider_webhook_events_provider_transaction
    on provider_webhook_events (provider, provider_transaction_id);

create index ix_provider_webhook_events_status_received
    on provider_webhook_events (processing_status, received_at);
