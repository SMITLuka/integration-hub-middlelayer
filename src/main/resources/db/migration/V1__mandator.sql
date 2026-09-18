CREATE TABLE mandator
(
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    system               VARCHAR(255),
    customer             VARCHAR(255),
    external_mandator_id VARCHAR(255),
    country              VARCHAR(255),
    locale               VARCHAR(255),
    created_at           TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_mandator_external_id UNIQUE (external_mandator_id)
);

CREATE TABLE mandator_additional_data
(
    mandator_id BIGINT       NOT NULL REFERENCES mandator (id) ON DELETE CASCADE,
    data_key    VARCHAR(255) NOT NULL,
    data_value  VARCHAR(2000),
    PRIMARY KEY (mandator_id, data_key)
);
