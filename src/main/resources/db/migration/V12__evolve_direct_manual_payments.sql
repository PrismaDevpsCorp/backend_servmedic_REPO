-- ============================================================
-- BLOQUE 38.4
-- Pago directo paciente -> especialista con evidencia manual
-- ============================================================

-- MedicDrive NO procesa ni custodia el dinero.
-- La evidencia es registrada por el paciente.
-- El especialista verifica externamente el pago.

ALTER TABLE medical_payments
    ADD COLUMN payment_flow VARCHAR(40) NOT NULL DEFAULT 'LEGACY';

ALTER TABLE medical_payments
    ADD COLUMN service_amount NUMERIC(10,2);

ALTER TABLE medical_payments
    ADD COLUMN mobility_amount NUMERIC(10,2);

ALTER TABLE medical_payments
    ADD COLUMN additional_amount NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE medical_payments
    ADD COLUMN evidence_file_name VARCHAR(255);

ALTER TABLE medical_payments
    ADD COLUMN evidence_content_type VARCHAR(100);

ALTER TABLE medical_payments
    ADD COLUMN evidence_size BIGINT;

ALTER TABLE medical_payments
    ADD COLUMN evidence_data BYTEA;

ALTER TABLE medical_payments
    ADD COLUMN evidence_uploaded_at TIMESTAMPTZ;

ALTER TABLE medical_payments
    ADD COLUMN verified_at TIMESTAMPTZ;

ALTER TABLE medical_payments
    ADD COLUMN verified_by_specialist_profile_id BIGINT;

ALTER TABLE medical_payments
    ADD COLUMN verification_warning_acknowledged BOOLEAN
        NOT NULL DEFAULT FALSE;

-- ------------------------------------------------------------
-- Compatibilidad con pagos historicos V6
-- ------------------------------------------------------------

UPDATE medical_payments mp
SET
    service_amount = COALESCE(proposal.service_amount, mp.amount),
    mobility_amount = COALESCE(proposal.mobility_amount, 0),
    additional_amount = CASE
        WHEN proposal.id IS NULL THEN 0
        ELSE GREATEST(mp.amount - proposal.total_amount, 0)
    END
FROM medical_requests request
LEFT JOIN medical_request_proposals proposal
    ON proposal.id = request.accepted_proposal_id
WHERE request.id = mp.medical_request_id;

ALTER TABLE medical_payments
    ALTER COLUMN service_amount SET NOT NULL;

ALTER TABLE medical_payments
    ALTER COLUMN mobility_amount SET NOT NULL;

-- Los nuevos pagos nacen pendientes.

ALTER TABLE medical_payments
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- paid_at solo existe cuando el especialista confirma.

ALTER TABLE medical_payments
    ALTER COLUMN paid_at DROP DEFAULT;

ALTER TABLE medical_payments
    ALTER COLUMN paid_at DROP NOT NULL;

ALTER TABLE medical_payments
    ADD CONSTRAINT fk_medical_payments_verified_specialist
    FOREIGN KEY (verified_by_specialist_profile_id)
    REFERENCES specialist_profiles(id);

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_flow
    CHECK (payment_flow IN ('LEGACY', 'DIRECT_EXTERNAL'));

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_direct_breakdown
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR (
            service_amount > 0
            AND mobility_amount >= 0
            AND additional_amount >= 0
            AND amount = service_amount
                + mobility_amount
                + additional_amount
            AND currency = 'PEN'
            AND payment_method IN (
                'YAPE',
                'PLIN',
                'TRANSFER',
                'CASH'
            )
        )
    );

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_direct_evidence
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR (
            evidence_file_name IS NOT NULL
            AND char_length(btrim(evidence_file_name)) BETWEEN 1 AND 255
            AND evidence_content_type IN (
                'image/jpeg',
                'image/png',
                'image/webp'
            )
            AND evidence_size BETWEEN 1 AND 5242880
            AND evidence_data IS NOT NULL
            AND evidence_uploaded_at IS NOT NULL
        )
    );

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
            )
            OR
            (
                status = 'PAID'
                AND paid_at IS NOT NULL
                AND verified_at IS NOT NULL
                AND verified_by_specialist_profile_id IS NOT NULL
                AND verification_warning_acknowledged = TRUE
            )
        )
    );

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_medical_payments_direct_commission
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR (
            platform_commission_percent = 5.00
            AND (
                (
                    status = 'PENDING'
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

CREATE INDEX idx_medical_payments_status_flow
    ON medical_payments (status, payment_flow);

CREATE INDEX idx_medical_payments_specialist_status_v12
    ON medical_payments (specialist_profile_id, status);
-- ============================================================
-- Integridad transaccional del pago directo
-- ============================================================

ALTER TABLE medical_payments
    ADD CONSTRAINT chk_direct_payment_verifier
    CHECK (
        payment_flow <> 'DIRECT_EXTERNAL'
        OR status <> 'PAID'
        OR verified_by_specialist_profile_id = specialist_profile_id
    );

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
    WHERE additional.medical_request_id = NEW.medical_request_id
      AND additional.status = 'APPROVED';

    IF NEW.service_amount IS DISTINCT FROM proposal_service_amount THEN
        RAISE EXCEPTION
            'El importe base del pago no coincide con la propuesta aceptada.';
    END IF;

    IF NEW.mobility_amount IS DISTINCT FROM proposal_mobility_amount THEN
        RAISE EXCEPTION
            'La movilidad del pago no coincide con la propuesta aceptada.';
    END IF;

    IF NEW.additional_amount IS DISTINCT FROM approved_additional_amount THEN
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

    IF NEW.medical_request_id IS DISTINCT FROM OLD.medical_request_id
       OR NEW.patient_profile_id IS DISTINCT FROM OLD.patient_profile_id
       OR NEW.specialist_profile_id IS DISTINCT FROM OLD.specialist_profile_id
       OR NEW.payment_flow IS DISTINCT FROM OLD.payment_flow
       OR NEW.service_amount IS DISTINCT FROM OLD.service_amount
       OR NEW.mobility_amount IS DISTINCT FROM OLD.mobility_amount
       OR NEW.additional_amount IS DISTINCT FROM OLD.additional_amount
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.payment_method IS DISTINCT FROM OLD.payment_method
       OR NEW.external_transaction_id IS DISTINCT FROM OLD.external_transaction_id
       OR NEW.evidence_file_name IS DISTINCT FROM OLD.evidence_file_name
       OR NEW.evidence_content_type IS DISTINCT FROM OLD.evidence_content_type
       OR NEW.evidence_size IS DISTINCT FROM OLD.evidence_size
       OR NEW.evidence_data IS DISTINCT FROM OLD.evidence_data
       OR NEW.evidence_uploaded_at IS DISTINCT FROM OLD.evidence_uploaded_at
       OR NEW.created_at IS DISTINCT FROM OLD.created_at
    THEN
        RAISE EXCEPTION
            'La evidencia, identidad y datos economicos del pago directo son inmutables.';
    END IF;

    IF OLD.status <> 'PENDING' THEN
        RAISE EXCEPTION
            'Un pago directo confirmado no puede modificarse. Estado actual: %.',
            OLD.status;
    END IF;

    IF NEW.status <> 'PAID' THEN
        RAISE EXCEPTION
            'Transicion de pago no permitida: % -> %.',
            OLD.status,
            NEW.status;
    END IF;

    IF NEW.verified_by_specialist_profile_id IS DISTINCT FROM NEW.specialist_profile_id THEN
        RAISE EXCEPTION
            'Solo el especialista asignado puede confirmar el pago.';
    END IF;

    IF NEW.verification_warning_acknowledged IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION
            'La advertencia de verificacion debe ser aceptada.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_direct_medical_payment
BEFORE INSERT OR UPDATE
ON medical_payments
FOR EACH ROW
EXECUTE FUNCTION validate_direct_medical_payment();
