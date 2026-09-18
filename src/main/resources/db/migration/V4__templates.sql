CREATE TABLE mapping_template
(
    id           BIGSERIAL PRIMARY KEY,
    interface_id BIGINT    NOT NULL REFERENCES interface_definition (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_mapping_template_interface UNIQUE (interface_id)
);

CREATE TABLE mapping_template_section
(
    id                  BIGSERIAL PRIMARY KEY,
    mapping_template_id BIGINT       NOT NULL REFERENCES mapping_template (id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    sort_order          INTEGER
);

CREATE TABLE mapping_template_row
(
    id                BIGSERIAL PRIMARY KEY,
    section_id        BIGINT       NOT NULL REFERENCES mapping_template_section (id) ON DELETE CASCADE,
    descriptor        VARCHAR(255) NOT NULL,
    third_party_value VARCHAR(2000),
    sort_order        INTEGER
);

CREATE TABLE configuration_template
(
    id           BIGSERIAL PRIMARY KEY,
    interface_id BIGINT    NOT NULL REFERENCES interface_definition (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_configuration_template_interface UNIQUE (interface_id)
);

CREATE TABLE configuration_template_entry
(
    id                        BIGSERIAL PRIMARY KEY,
    configuration_template_id BIGINT       NOT NULL REFERENCES configuration_template (id) ON DELETE CASCADE,
    config_key                VARCHAR(255) NOT NULL,
    type                      VARCHAR(50)  NOT NULL,
    default_value             VARCHAR(2000),
    expression                VARCHAR(2000),
    description               VARCHAR(2000),
    sort_order                INTEGER,
    CONSTRAINT uk_configuration_template_entry_key UNIQUE (configuration_template_id, config_key)
);
