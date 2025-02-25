CREATE INDEX ocpp_charge_sess_tx_idx ON solarev.ocpp_charge_sess (tx_id) /*TABLESPACE solarindex*/;

CREATE INDEX ocpp_charge_sess_conn_idx ON solarev.ocpp_charge_sess (cp_id, evse_id, conn_id) /*TABLESPACE solarindex*/;
