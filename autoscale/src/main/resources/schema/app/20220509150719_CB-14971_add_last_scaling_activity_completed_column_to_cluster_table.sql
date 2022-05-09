-- // CB-14971 add last_scaling_activity_completed column to cluster table
-- Migration SQL that makes the change goes here.

alter table cluster add column if not exists last_scaling_activity_completed bigint;

-- //@UNDO
-- SQL to undo the change goes here.

alter table cluster drop column if exists last_scaling_activity_completed;
