-- ============================================================
-- MEDICDRIVE B42.6
-- Completar perfil comercial del especialista
-- ============================================================

-- ------------------------------------------------------------
-- 1. Datos de cobranza por metodo de pago
-- ------------------------------------------------------------

ALTER TABLE specialist_payment_methods
    ADD COLUMN IF NOT EXISTS mobile_phone VARCHAR(30);

ALTER TABLE specialist_payment_methods
    ADD COLUMN IF NOT EXISTS account_holder VARCHAR(160);

ALTER TABLE specialist_payment_methods
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(120);

ALTER TABLE specialist_payment_methods
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(80);

ALTER TABLE specialist_payment_methods
    ADD COLUMN IF NOT EXISTS cci VARCHAR(40);

-- ------------------------------------------------------------
-- 2. Completar catalogo profesional de especialistas existentes
--
-- Los servicios que antes no existian para un especialista se
-- incorporan INACTIVOS para no alterar de forma sorpresiva su
-- oferta comercial previa.
-- ------------------------------------------------------------

INSERT INTO specialist_offered_services (
    specialist_profile_id,
    medical_service_id,
    base_price,
    active
)
SELECT
    sp.id,
    ms.id,
    CASE p.code
        WHEN 'MEDICO_GENERAL' THEN 90.00
        WHEN 'ENFERMERIA' THEN 70.00
        WHEN 'TERAPIA_FISICA_REHABILITACION' THEN 80.00
        WHEN 'PSICOLOGIA' THEN 85.00
        ELSE 75.00
    END,
    FALSE
FROM specialist_profiles sp
JOIN professions p
    ON p.id = sp.profession_id
JOIN medical_services ms
    ON ms.profession_id = p.id
   AND ms.active = TRUE
WHERE p.active = TRUE
ON CONFLICT (
    specialist_profile_id,
    medical_service_id
)
DO NOTHING;

-- ------------------------------------------------------------
-- 3. Completar precios nulos existentes
-- ------------------------------------------------------------

UPDATE specialist_offered_services sos
SET base_price =
    CASE p.code
        WHEN 'MEDICO_GENERAL' THEN 90.00
        WHEN 'ENFERMERIA' THEN 70.00
        WHEN 'TERAPIA_FISICA_REHABILITACION' THEN 80.00
        WHEN 'PSICOLOGIA' THEN 85.00
        ELSE 75.00
    END
FROM specialist_profiles sp
JOIN professions p
    ON p.id = sp.profession_id
WHERE sos.specialist_profile_id = sp.id
  AND sos.base_price IS NULL;

-- ------------------------------------------------------------
-- 4. Especialistas creados despues de V9:
--    completar perfil comercial general faltante
-- ------------------------------------------------------------

INSERT INTO specialist_commercial_profiles (
    specialist_profile_id
)
SELECT
    sp.id
FROM specialist_profiles sp
ON CONFLICT (
    specialist_profile_id
)
DO NOTHING;

-- ------------------------------------------------------------
-- 5. Especialistas creados despues de V9:
--    garantizar CASH inicial sin modificar configuraciones
--    que ya existan.
-- ------------------------------------------------------------

INSERT INTO specialist_payment_methods (
    specialist_profile_id,
    payment_method_id,
    active
)
SELECT
    sp.id,
    pm.id,
    TRUE
FROM specialist_profiles sp
JOIN payment_methods pm
    ON pm.code = 'CASH'
   AND pm.active = TRUE
ON CONFLICT (
    specialist_profile_id,
    payment_method_id
)
DO NOTHING;
