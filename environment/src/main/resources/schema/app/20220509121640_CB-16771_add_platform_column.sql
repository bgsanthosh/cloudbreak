-- // CB-16771 introduce platform to differentiate PaaS and hybrid setup (PaaS Env + SaaS SDX)
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS platform VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS platform;