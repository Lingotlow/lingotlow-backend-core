-- Create events table
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    document_id TEXT,
    type TEXT,
    status TEXT NOT NULL, -- RECEIVED, ENQUEUED, PROCESSING, SENT, FAILED, DLQ
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    received_at TIMESTAMPTZ,
    s3_key TEXT,
    payload_json JSONB,
    headers JSONB,
    source_ip INET,
    attempts INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    response_code INT,
    response_body TEXT,
    
    CONSTRAINT fk_events_tenant 
        FOREIGN KEY(tenant_id) 
        REFERENCES tenants(id) 
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_events_tenant_status ON events(tenant_id, status);
CREATE INDEX idx_events_tenant_document ON events(tenant_id, document_id);
CREATE INDEX idx_events_request_id ON events(request_id);
CREATE INDEX idx_events_created_at ON events(created_at);

-- Add comments for documentation
COMMENT ON TABLE events IS 'Stores event metadata and processing status';
COMMENT ON COLUMN events.status IS 'Event processing status: RECEIVED, ENQUEUED, PROCESSING, SENT, FAILED, DLQ';
COMMENT ON COLUMN events.payload_json IS 'Event payload stored as JSONB for efficient querying';
COMMENT ON COLUMN events.headers IS 'HTTP headers stored as JSONB';
COMMENT ON COLUMN events.source_ip IS 'Client IP address for audit and analytics';
