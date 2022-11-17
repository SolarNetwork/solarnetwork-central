/*
DROP TABLE IF EXISTS solarev.ocpp_charge_point_status CASCADE;
DROP TABLE IF EXISTS solarev.ocpp_charge_point_action_status CASCADE;
*/

/**
 * Table for OCPP charger-wide status info.
 *
 * created 			the row creation date
 * user_id 			the account owner
 * cp_id			the charge point ID
 * connected_to		the name of the SolarIn instance connected to, NULL if not connected
 * connected_ts		the date the charger last connected
 */
CREATE TABLE solarev.ocpp_charge_point_status (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cp_id			BIGINT NOT NULL,
	connected_to	TEXT,
	connected_date	TIMESTAMP WITH TIME ZONE,
	CONSTRAINT ocpp_charge_point_status_pk PRIMARY KEY (user_id, cp_id),
	CONSTRAINT ocpp_charge_point_status_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id)
		ON DELETE CASCADE
);

/**
 * Table for OCPP "last seen" timestamp for each action of a charger.
 *
 * created 			the row creation date
 * user_id 			the account owner
 * cp_id			the charge point ID
 * conn_id			the connector ID (>= 0) related to the action, or 0 for the charger
 * action			the name of the OCPP action
 * msg_id			the action message ID
 * ts				the  date the action occurred at
 */
CREATE TABLE solarev.ocpp_charge_point_action_status (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cp_id			BIGINT NOT NULL,
	conn_id			INTEGER NOT NULL,
	action			TEXT NOT NULL,
	msg_id			TEXT NOT NULL,
	ts				TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT ocpp_charge_point_action_status_pk PRIMARY KEY (user_id, cp_id, conn_id, action),
	CONSTRAINT ocpp_charge_point_action_status_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id)
		ON DELETE CASCADE
);
