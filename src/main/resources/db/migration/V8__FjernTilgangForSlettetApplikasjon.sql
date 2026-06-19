DO $$BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='helse-sparker-feriepenger')
    THEN
        REVOKE SELECT ON melding FROM "helse-sparker-feriepenger";
    END IF;
END$$;
