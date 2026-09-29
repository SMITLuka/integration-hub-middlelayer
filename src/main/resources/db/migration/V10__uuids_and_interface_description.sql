ALTER TABLE mandator ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mandator ADD CONSTRAINT uk_mandator_uuid UNIQUE (uuid);

ALTER TABLE company ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE company ADD CONSTRAINT uk_company_uuid UNIQUE (uuid);

ALTER TABLE interface_definition ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE interface_definition ADD CONSTRAINT uk_interface_uuid UNIQUE (uuid);

ALTER TABLE interface_definition DROP COLUMN dms_to_middleware_url;
ALTER TABLE interface_definition DROP COLUMN oem_to_middleware_url;
ALTER TABLE interface_definition DROP COLUMN middleware_to_oem_url;
ALTER TABLE interface_definition ADD COLUMN description VARCHAR(2000);
