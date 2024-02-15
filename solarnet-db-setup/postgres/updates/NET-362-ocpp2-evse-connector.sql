-- add evse_id to connector table

ALTER TABLE solarev.ocpp_charge_point_conn
ADD COLUMN evse_id INTEGER NOT NULL DEFAULT 0;

ALTER TABLE solarev.ocpp_charge_point_conn
DROP CONSTRAINT ocpp_charge_point_conn_pk;

ALTER TABLE solarev.ocpp_charge_point_conn
ADD CONSTRAINT ocpp_charge_point_conn_pk PRIMARY KEY (cp_id, evse_id, conn_id);

-- add evse_id to connector status table

ALTER TABLE solarev.ocpp_charge_point_action_status
ADD COLUMN evse_id INTEGER NOT NULL DEFAULT 0;

ALTER TABLE solarev.ocpp_charge_point_action_status
DROP CONSTRAINT ocpp_charge_point_action_status_pk;

ALTER TABLE solarev.ocpp_charge_point_action_status
ADD CONSTRAINT ocpp_charge_point_action_status_pk PRIMARY KEY (user_id, cp_id, evse_id, conn_id, action);

-- add evse_id to charge session table

ALTER TABLE solarev.ocpp_charge_sess
ADD COLUMN evse_id INTEGER NOT NULL DEFAULT 0;

-- change charge session transaction ID to text

ALTER TABLE solarev.ocpp_charge_sess
ALTER COLUMN tx_id SET DATA TYPE VARCHAR(36);
