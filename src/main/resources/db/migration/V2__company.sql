CREATE TABLE company
(
    id               BIGSERIAL PRIMARY KEY,
    mandator_id      BIGINT       NOT NULL REFERENCES mandator (id),
    name             VARCHAR(255) NOT NULL,
    dms_company_id   VARCHAR(255),
    location         VARCHAR(255),
    country_code     VARCHAR(255),
    customer_number  VARCHAR(255),
    default_locale   VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_company_mandator_dms_id UNIQUE (mandator_id, dms_company_id)
);

CREATE TABLE company_additional_data
(
    company_id BIGINT       NOT NULL REFERENCES company (id) ON DELETE CASCADE,
    data_key   VARCHAR(255) NOT NULL,
    data_value VARCHAR(2000),
    PRIMARY KEY (company_id, data_key)
);
