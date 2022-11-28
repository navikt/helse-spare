ALTER TABLE melding ADD COLUMN aktor_id BIGINT;
UPDATE melding SET aktor_id = ((json #>> '{}')::json ->> 'aktørId')::int;
ALTER TABLE melding ALTER COLUMN aktor_id SET NOT NULL;

CREATE INDEX melding_aktor_id_fnr_idx ON melding (aktor_id, fnr);
