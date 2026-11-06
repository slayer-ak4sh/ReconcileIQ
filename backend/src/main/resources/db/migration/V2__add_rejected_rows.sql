-- Add rejected_rows table
CREATE TABLE rejected_rows (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES upload_batches(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    raw_data_json TEXT NOT NULL,
    error_reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rejected_rows_batch_id ON rejected_rows(batch_id);