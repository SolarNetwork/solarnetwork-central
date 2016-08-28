DROP VIEW IF EXISTS quartz.cron_trigger_view;

--
-- drop tables that are no longer used
--
drop table quartz.job_listeners;
drop table quartz.trigger_listeners;
--
-- drop columns that are no longer used
--
alter table quartz.job_details drop column is_volatile;
alter table quartz.triggers drop column is_volatile;
alter table quartz.fired_triggers drop column is_volatile;
--
-- add new columns that replace the 'is_stateful' column
--
alter table quartz.job_details add column is_nonconcurrent bool;
alter table quartz.job_details add column is_update_data bool;
update quartz.job_details set is_nonconcurrent = is_stateful;
update quartz.job_details set is_update_data = is_stateful;
alter table quartz.job_details drop column is_stateful;
alter table quartz.fired_triggers add column is_nonconcurrent bool;
alter table quartz.fired_triggers add column is_update_data bool;
update quartz.fired_triggers set is_nonconcurrent = is_stateful;
update quartz.fired_triggers set is_update_data = is_stateful;
alter table quartz.fired_triggers drop column is_stateful;
--
-- add new 'sched_name' column to all tables
--
alter table quartz.blob_triggers add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.calendars add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.cron_triggers add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.fired_triggers add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.fired_triggers add column sched_time BIGINT NOT NULL DEFAULT 0;
alter table quartz.job_details add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.locks add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.paused_trigger_grps add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.scheduler_state add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.simple_triggers add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
alter table quartz.triggers add column sched_name varchar(120) not null DEFAULT 'TestScheduler';
--
-- drop all primary and foreign key constraints, so that we can define new ones
--
alter table quartz.triggers drop constraint triggers_job_name_fkey;
alter table quartz.blob_triggers drop constraint blob_triggers_pkey;
alter table quartz.blob_triggers drop constraint blob_triggers_trigger_name_fkey;
alter table quartz.simple_triggers drop constraint simple_triggers_pkey;
alter table quartz.simple_triggers drop constraint simple_triggers_trigger_name_fkey;
alter table quartz.cron_triggers drop constraint cron_triggers_pkey;
alter table quartz.cron_triggers drop constraint cron_triggers_trigger_name_fkey;
alter table quartz.job_details drop constraint job_details_pkey;
alter table quartz.job_details add primary key (sched_name, job_name, job_group);
alter table quartz.triggers drop constraint triggers_pkey;
--
-- add all primary and foreign key constraints, based on new columns
--
alter table quartz.triggers add primary key (sched_name, trigger_name, trigger_group);
alter table quartz.triggers add foreign key (sched_name, job_name, job_group) references quartz.job_details(sched_name, job_name, job_group);
alter table quartz.blob_triggers add primary key (sched_name, trigger_name, trigger_group);
alter table quartz.blob_triggers add foreign key (sched_name, trigger_name, trigger_group) references quartz.triggers(sched_name, trigger_name, trigger_group);
alter table quartz.cron_triggers add primary key (sched_name, trigger_name, trigger_group);
alter table quartz.cron_triggers add foreign key (sched_name, trigger_name, trigger_group) references quartz.triggers(sched_name, trigger_name, trigger_group);
alter table quartz.simple_triggers add primary key (sched_name, trigger_name, trigger_group);
alter table quartz.simple_triggers add foreign key (sched_name, trigger_name, trigger_group) references quartz.triggers(sched_name, trigger_name, trigger_group);
alter table quartz.fired_triggers drop constraint fired_triggers_pkey;
alter table quartz.fired_triggers add primary key (sched_name, entry_id);
alter table quartz.calendars drop constraint calendars_pkey;
alter table quartz.calendars add primary key (sched_name, calendar_name);
alter table quartz.locks drop constraint locks_pkey;
alter table quartz.locks add primary key (sched_name, lock_name);
alter table quartz.paused_trigger_grps drop constraint paused_trigger_grps_pkey;
alter table quartz.paused_trigger_grps add primary key (sched_name, trigger_group);
alter table quartz.scheduler_state drop constraint scheduler_state_pkey;
alter table quartz.scheduler_state add primary key (sched_name, instance_name);
--
-- add new simprop_triggers table
--
CREATE TABLE quartz.simprop_triggers
 (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOL NULL,
    BOOL_PROP_2 BOOL NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES quartz.TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
--
-- create indexes for faster queries
--
create index idx_qrtz_j_req_recovery on quartz.job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on quartz.job_details(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_j on quartz.triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on quartz.triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on quartz.triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on quartz.triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on quartz.triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on quartz.triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on quartz.triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on quartz.triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on quartz.triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on quartz.triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on quartz.triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on quartz.triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_ft_trig_inst_name on quartz.fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on quartz.fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on quartz.fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on quartz.fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on quartz.fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on quartz.fired_triggers(SCHED_NAME,TRIGGER_GROUP);

-- Custom additions

CREATE OR REPLACE VIEW quartz.cron_trigger_view AS
SELECT t.trigger_name, t.trigger_group, t.job_name, t.job_group,
	c.cron_expression,
	to_timestamp(t.next_fire_time / 1000) as next_fire_time,
	to_timestamp(t.prev_fire_time / 1000) as prev_fire_time,
	t.priority, t.trigger_state,
	CASE t.start_time WHEN 0 THEN NULL ELSE to_timestamp(t.start_time / 1000) END as start_time,
	CASE t.end_time WHEN 0 THEN NULL ELSE to_timestamp(t.end_time / 1000) END as end_time
FROM quartz.triggers t
INNER JOIN quartz.cron_triggers c ON c.trigger_name = t.trigger_name AND c.trigger_group = t.trigger_group
ORDER BY t.trigger_group, t.trigger_name, t.job_group, t.job_name;
