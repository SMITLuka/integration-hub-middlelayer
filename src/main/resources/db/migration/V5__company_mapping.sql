CREATE TABLE company_mapping
(
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT    NOT NULL REFERENCES company (id),
    interface_id BIGINT    NOT NULL REFERENCES interface_definition (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_company_mapping UNIQUE (company_id, interface_id)
);

CREATE TABLE company_mapping_value
(
    id                 BIGSERIAL PRIMARY KEY,
    company_mapping_id BIGINT NOT NULL REFERENCES company_mapping (id) ON DELETE CASCADE,
    template_row_id    BIGINT NOT NULL REFERENCES mapping_template_row (id),
    override_value     VARCHAR(2000),
    CONSTRAINT uk_company_mapping_value UNIQUE (company_mapping_id, template_row_id)
);
