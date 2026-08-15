-- ============================================================
-- BLOQUE 38.5
-- Rechazo, trazabilidad y reenvio de pago manual directo
-- ============================================================

-- MedicDrive sigue siendo NO custodial.
-- El rechazo no mueve dinero ni genera comision.
-- El intento rechazado se archiva antes de permitir reenvio.

ALTER TABLE medical_payments
    ADD COLUMN rejected_at TIMESTAMPTZ;

ALTER TABLE medical_payments
    ADD COLUMN rejected_by_specialist_profile_id BIGINT;

ALTER TABLE medical_payments
    ADD COLUMN rejection_reason VARCHAR(500);

ALTER TABLE medical_payments
    ADD CONSTRAINT fk_medical_payments_rejected_specialist
    FOREIGN KEY (rejected_by_specialist_profile_id)
    REFERENCES specialist_profiles(id);

ALTER TABLE medical_payments
    DROP CONSTRAINT IF EXISTS chk_medical_payments_status;

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_status
    CHECK (
        status IN (
            'PENDING',
            'PAID',
            'REJECTED',
            'FAILED',
            'REFUNDED'
        )
    );

ALTER TABLE medical_payments
    DROP CONSTRAINT IF EXISTS chk_medical_payments_direct_state;

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_direct_state
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR (
            (
                status = 'PENDING'
                AND paid_at IS NULL
                AND verified_at IS NULL
                AND verified_by_specialist_profile_id IS NULL
                AND verification_warning_acknowledged = FALSE
                AND rejected_at IS NULL
                AND rejected_by_specialist_profile_id IS NULL
                AND rejection_reason IS NULL
            )
            OR
            (
                status = 'PAID'
                AND paid_at IS NOT NULL
                AND verified_at IS NOT NULL
                AND verified_by_specialist_profile_id IS NOT NULL
                AND verification_warning_acknowledged = TRUE
                AND rejected_at IS NULL
                AND rejected_by_specialist_profile_id IS NULL
                AND rejection_reason IS NULL
            )
            OR
            (
                status = 'REJECTED'
                AND paid_at IS NULL
                AND verified_at IS NULL
                AND verified_by_specialist_profile_id IS NULL
                AND verification_warning_acknowledged = FALSE
                AND rejected_at IS NOT NULL
                AND rejected_by_specialist_profile_id IS NOT NULL
                AND rejection_reason IS NOT NULL
                AND char_length(btrim(rejection_reason))
                    BETWEEN 1 AND 500
            )
        )
    );

ALTER TABLE medical_payments
    DROP CONSTRAINT IF EXISTS chk_medical_payments_direct_commission;

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_direct_commission
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR (
            platform_commission_percent = 5.00
            AND (
                (
                    status IN ('PENDING', 'REJECTED')
                    AND platform_commission_amount = 0
                    AND specialist_net_amount = amount
                )
                OR
                (
                    status = 'PAID'
                    AND platform_commission_amount =
                        ROUND(service_amount * 0.05, 2)
                    AND specialist_net_amount =
                        amount - ROUND(service_amount * 0.05, 2)
                )
            )
        )
    );

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_direct_payment_rejector
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR status <> 'REJECTED'
        OR rejected_by_specialist_profile_id =
            specialist_profile_id
    );

CREATE TABLE medical_payment_attempts (
    id BIGSERIAL PRIMARY KEY,

    medical_payment_id BIGINT NOT NULL,

    attempt_number INTEGER NOT NULL,

    payment_method VARCHAR(40) NOT NULL,
    external_transaction_id VARCHAR(120),

    evidence_file_name VARCHAR(255) NOT NULL,
    evidence_content_type VARCHAR(100) NOT NULL,
    evidence_size BIGINT NOT NULL,
    evidence_data BYTEA NOT NULL,
    evidence_uploaded_at TIMESTAMPTZ NOT NULL,

    rejected_at TIMESTAMPTZ NOT NULL,
    rejected_by_specialist_profile_id BIGINT NOT NULL,
    rejection_reason VARCHAR(500) NOT NULL,

    archived_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_attempt_payment
        FOREIGN KEY (medical_payment_id)
        REFERENCES medical_payments(id),

    CONSTRAINT fk_payment_attempt_rejected_specialist
        FOREIGN KEY (rejected_by_specialist_profile_id)
        REFERENCES specialist_profiles(id),

    CONSTRAINT uq_medical_payment_attempt_number
        UNIQUE (
            medical_payment_id,
            attempt_number
        ),

    CONSTRAINT chk_payment_attempt_number
        CHECK (attempt_number > 0),

    CONSTRAINT chk_payment_attempt_evidence
        CHECK (
            evidence_content_type IN (
                'image/jpeg',
                'image/png',
                'image/webp'
            )
            AND evidence_size BETWEEN 1 AND 5242880
        ),

    CONSTRAINT chk_payment_attempt_reason
        CHECK (
            char_length(btrim(rejection_reason))
                BETWEEN 1 AND 500
        )
);

CREATE INDEX idx_payment_attempt_payment
    ON medical_payment_attempts (
        medical_payment_id,
        attempt_number
    );

CREATE INDEX idx_payment_attempt_rejected_at
    ON medical_payment_attempts (
        rejected_at
    );

-- ------------------------------------------------------------
-- Integridad transaccional B38.5
-- ------------------------------------------------------------

CREATE OR REPLACE FUNCTION validate_direct_medical_payment()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    request_patient_id BIGINT;
    request_specialist_id BIGINT;
    request_status VARCHAR(40);
    request_proposal_id BIGINT;

    proposal_service_amount NUMERIC(10,2);
    proposal_mobility_amount NUMERIC(10,2);
    approved_additional_amount NUMERIC(10,2);
BEGIN
    IF NEW.payment_flow <> 'DIRECT_EXTERNAL' THEN
        RETURN NEW;
    END IF;

    SELECT
        patient_profile_id,
        accepted_specialist_profile_id,
        status,
        accepted_proposal_id
    INTO
        request_patient_id,
        request_specialist_id,
        request_status,
        request_proposal_id
    FROM medical_requests
    WHERE id = NEW.medical_request_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'La solicitud medica % no existe.',
            NEW.medical_request_id;
    END IF;

    IF request_status <> 'FINALIZADO' THEN
        RAISE EXCEPTION
            'El pago directo solo puede registrarse para una solicitud FINALIZADO. Estado actual: %.',
            request_status;
    END IF;

    IF request_patient_id <> NEW.patient_profile_id THEN
        RAISE EXCEPTION
            'El paciente del pago no corresponde a la solicitud.';
    END IF;

    IF request_specialist_id IS NULL
       OR request_specialist_id <> NEW.specialist_profile_id
    THEN
        RAISE EXCEPTION
            'El especialista del pago no corresponde al especialista asignado.';
    END IF;

    IF request_proposal_id IS NULL THEN
        RAISE EXCEPTION
            'La solicitud no tiene propuesta aceptada.';
    END IF;

    SELECT
        proposal.service_amount,
        proposal.mobility_amount
    INTO
        proposal_service_amount,
        proposal_mobility_amount
    FROM medical_request_proposals proposal
    WHERE proposal.id = request_proposal_id
      AND proposal.medical_request_id = NEW.medical_request_id
      AND proposal.specialist_profile_id = NEW.specialist_profile_id
      AND proposal.status = 'ACCEPTED';

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'La propuesta aceptada del pago no es valida.';
    END IF;

    SELECT
        COALESCE(SUM(additional.amount), 0)
    INTO
        approved_additional_amount
    FROM medical_request_additionals additional
    WHERE additional.medical_request_id =
        NEW.medical_request_id
      AND additional.status = 'APPROVED';

    IF NEW.service_amount IS DISTINCT FROM
        proposal_service_amount
    THEN
        RAISE EXCEPTION
            'El importe base del pago no coincide con la propuesta aceptada.';
    END IF;

    IF NEW.mobility_amount IS DISTINCT FROM
        proposal_mobility_amount
    THEN
        RAISE EXCEPTION
            'La movilidad del pago no coincide con la propuesta aceptada.';
    END IF;

    IF NEW.additional_amount IS DISTINCT FROM
        approved_additional_amount
    THEN
        RAISE EXCEPTION
            'Los adicionales del pago no coinciden con los adicionales aprobados.';
    END IF;

    IF TG_OP = 'INSERT' THEN

        IF NEW.status <> 'PENDING' THEN
            RAISE EXCEPTION
                'Un pago directo debe iniciar en estado PENDING.';
        END IF;

        RETURN NEW;
    END IF;

    -- Identidad y economia nunca cambian.

    IF NEW.medical_request_id IS DISTINCT FROM
        OLD.medical_request_id
       OR NEW.patient_profile_id IS DISTINCT FROM
        OLD.patient_profile_id
       OR NEW.specialist_profile_id IS DISTINCT FROM
        OLD.specialist_profile_id
       OR NEW.payment_flow IS DISTINCT FROM
        OLD.payment_flow
       OR NEW.service_amount IS DISTINCT FROM
        OLD.service_amount
       OR NEW.mobility_amount IS DISTINCT FROM
        OLD.mobility_amount
       OR NEW.additional_amount IS DISTINCT FROM
        OLD.additional_amount
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.platform_commission_percent IS DISTINCT FROM
        OLD.platform_commission_percent
       OR NEW.created_at IS DISTINCT FROM OLD.created_at
    THEN
        RAISE EXCEPTION
            'La identidad y datos economicos del pago directo son inmutables.';
    END IF;

    -- Un intento PENDING conserva exactamente su evidencia
    -- mientras el especialista decide.

    IF OLD.status = 'PENDING' THEN

        IF NEW.payment_method IS DISTINCT FROM
            OLD.payment_method
           OR NEW.external_transaction_id IS DISTINCT FROM
            OLD.external_transaction_id
           OR NEW.evidence_file_name IS DISTINCT FROM
            OLD.evidence_file_name
           OR NEW.evidence_content_type IS DISTINCT FROM
            OLD.evidence_content_type
           OR NEW.evidence_size IS DISTINCT FROM
            OLD.evidence_size
           OR NEW.evidence_data IS DISTINCT FROM
            OLD.evidence_data
           OR NEW.evidence_uploaded_at IS DISTINCT FROM
            OLD.evidence_uploaded_at
        THEN
            RAISE EXCEPTION
                'La evidencia pendiente no puede modificarse durante la verificacion.';
        END IF;

        IF NEW.status = 'PAID' THEN
            RETURN NEW;
        END IF;

        IF NEW.status = 'REJECTED' THEN
            RETURN NEW;
        END IF;

        RAISE EXCEPTION
            'Transicion de pago no permitida: % -> %.',
            OLD.status,
            NEW.status;
    END IF;

    -- Tras rechazo, el paciente puede presentar
    -- un nuevo intento. La evidencia previa ya fue
    -- archivada en medical_payment_attempts.

    IF OLD.status = 'REJECTED' THEN

        IF NEW.status <> 'PENDING' THEN
            RAISE EXCEPTION
                'Un pago rechazado solo puede volver a PENDING mediante un nuevo intento.';
        END IF;

        RETURN NEW;
    END IF;

    RAISE EXCEPTION
        'Un pago directo cerrado no puede modificarse. Estado actual: %.',
        OLD.status;
END;
$$;

-- El trigger creado por V12 continua apuntando
-- a validate_direct_medical_payment(), cuyo cuerpo
-- acaba de evolucionar mediante CREATE OR REPLACE.
