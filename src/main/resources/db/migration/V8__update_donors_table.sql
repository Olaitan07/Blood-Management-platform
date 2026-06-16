-- Drop old blood_type column and replace with blood_group + new fields
ALTER TABLE donors
    DROP COLUMN IF EXISTS blood_type;

ALTER TABLE donors
    ADD COLUMN blood_group         VARCHAR(5)   NOT NULL DEFAULT 'O+',
    ADD COLUMN phone               VARCHAR(20)  NOT NULL DEFAULT '',
    ADD COLUMN user_email          VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN eligibility_status  VARCHAR(15)  NOT NULL DEFAULT 'ELIGIBLE',
    ADD COLUMN last_donation_date  DATE;

ALTER TABLE donors
    ADD CONSTRAINT donors_phone_unique      UNIQUE (phone),
    ADD CONSTRAINT donors_user_email_unique UNIQUE (user_email);
