ALTER TABLE mandator DROP COLUMN customer;

ALTER TABLE mandator ADD COLUMN host_url VARCHAR(255);
ALTER TABLE mandator ADD COLUMN port INTEGER;
ALTER TABLE mandator ADD COLUMN personal_identification_number VARCHAR(255);
