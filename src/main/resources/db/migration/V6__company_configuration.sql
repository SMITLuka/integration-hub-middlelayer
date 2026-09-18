CREATE TABLE company_configuration
(
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT    NOT NULL REFERENCES company (id),
    interface_id BIGINT    NOT NULL REFERENCES interface_definition (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_company_configuration UNIQUE (company_id, interface_id)
);

CREATE TABLE company_configuration_override
(
    id                       BIGSERIAL PRIMARY KEY,
    company_configuration_id BIGINT NOT NULL REFERENCES company_configuration (id) ON DELETE CASCADE,
    template_entry_id        BIGINT NOT NULL REFERENCES configuration_template_entry (id),
    override_value           VARCHAR(2000),
    CONSTRAINT uk_company_configuration_override UNIQUE (company_configuration_id, template_entry_id)
);
