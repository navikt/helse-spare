CREATE TABLE melding_type
(
    id   SERIAL PRIMARY KEY,
    navn VARCHAR(32) UNIQUE NOT NULL
);

CREATE TABLE melding
(
    id              UUID PRIMARY KEY,
    melding_type_id INT       NOT NULL REFERENCES melding_type (id) ON DELETE RESTRICT,
    opprettet       TIMESTAMP NOT NULL,
    fnr             BIGINT    NOT NULL,
    json            JSON      NOT NULL
);

CREATE INDEX melding_fnr_idx ON melding (fnr);
CREATE INDEX melding_type_id_idx ON melding (melding_type_id);
