ALTER TABLE medical_requests
ADD COLUMN IF NOT EXISTS accepted_specialist_profile_id BIGINT;

ALTER TABLE medical_requests
ADD CONSTRAINT fk_medical_requests_accepted_specialist
FOREIGN KEY (accepted_specialist_profile_id)
REFERENCES specialist_profiles(id);

CREATE INDEX IF NOT EXISTS idx_medical_requests_accepted_specialist
ON medical_requests (accepted_specialist_profile_id);