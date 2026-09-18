CREATE TABLE interface_definition
(
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    dms_to_middleware_url   VARCHAR(2000),
    oem_to_middleware_url   VARCHAR(2000),
    middleware_to_oem_url   VARCHAR(2000),
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_interface_name UNIQUE (name)
);

CREATE TABLE interface_additional_data
(
    interface_id BIGINT       NOT NULL REFERENCES interface_definition (id) ON DELETE CASCADE,
    data_key     VARCHAR(255) NOT NULL,
    data_value   VARCHAR(2000),
    PRIMARY KEY (interface_id, data_key)
);
