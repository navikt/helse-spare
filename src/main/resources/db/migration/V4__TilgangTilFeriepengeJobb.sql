
DO
$$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='helse-sparker-feriepenger') THEN
        GRANT SELECT ON melding TO "helse-sparker-feriepenger";
    END IF;
END
$$
