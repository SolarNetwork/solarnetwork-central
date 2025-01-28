CREATE OR REPLACE FUNCTION solarbill.billing_usage_tier_effective_dates()
	RETURNS TABLE(
		effective_date DATE
	)
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT unnest(ARRAY[
		  '2008-01-01'::DATE
		, '2020-06-01'::DATE
		, '2021-06-01'::DATE
		, '2022-10-01'::DATE
		, '2023-10-01'::DATE
		, '2024-02-01'::DATE
		, '2024-10-01'::DATE
		, '2024-11-01'::DATE
		, '2025-02-01'::DATE
	]);
$$;

CREATE OR REPLACE FUNCTION solarbill.billing_usage_tiers(ts date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		meter_key TEXT,
		min BIGINT,
		cost NUMERIC,
		effective_date DATE
	)
	LANGUAGE plpgsql IMMUTABLE AS
$$
BEGIN
	IF ts < '2020-06-01'::DATE THEN
		RETURN QUERY SELECT *, '2008-01-01'::DATE AS effective_date FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 	0.000009::NUMERIC)
			, ('datum-out', 			0::BIGINT, 	0.000002::NUMERIC)
			, ('datum-days-stored', 	0::BIGINT, 	0.000000006::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSIF ts < '2021-06-01'::DATE THEN
		RETURN QUERY SELECT *, '2020-06-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 			0.000009::NUMERIC)
			, ('datum-props-in', 		50000::BIGINT, 		0.000006::NUMERIC)
			, ('datum-props-in', 		400000::BIGINT, 	0.000004::NUMERIC)
			, ('datum-props-in', 		1000000::BIGINT, 	0.000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 			0.000002::NUMERIC)
			, ('datum-out',				50000::BIGINT, 		0.000001::NUMERIC)
			, ('datum-out',				400000::BIGINT, 	0.0000005::NUMERIC)
			, ('datum-out',				1000000::BIGINT, 	0.0000002::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 			0.0000004::NUMERIC)
			, ('datum-days-stored', 	50000::BIGINT, 		0.0000002::NUMERIC)
			, ('datum-days-stored', 	400000::BIGINT, 	0.00000005::NUMERIC)
			, ('datum-days-stored', 	1000000::BIGINT, 	0.000000006::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSIF ts < '2022-10-01'::DATE THEN
		RETURN QUERY SELECT *, '2021-06-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSIF ts < '2023-10-01'::DATE THEN
		RETURN QUERY SELECT *, '2022-10-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				50::NUMERIC)
			, ('oscp-cap-groups', 		30::BIGINT, 			30::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			15::NUMERIC)
			, ('oscp-cap-groups', 		300::BIGINT, 			10::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2024-02-01'::DATE THEN
		RETURN QUERY SELECT *, '2023-10-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				50::NUMERIC)
			, ('oscp-cap-groups', 		30::BIGINT, 			30::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			15::NUMERIC)
			, ('oscp-cap-groups', 		300::BIGINT, 			10::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2024-10-01'::DATE THEN
		RETURN QUERY SELECT *, '2024-02-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2024-11-01'::DATE THEN
		RETURN QUERY SELECT *, '2024-10-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('flux-data-in', 			0::BIGINT, 				0.00000001::NUMERIC)
			, ('flux-data-in', 			1000000000::BIGINT, 	0.000000006::NUMERIC)
			, ('flux-data-in', 			10000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('flux-data-in', 			100000000000::BIGINT,	0.0000000015::NUMERIC)

			, ('flux-data-out', 		0::BIGINT, 				0.000000009::NUMERIC)
			, ('flux-data-out', 		1000000000::BIGINT, 	0.0000000055::NUMERIC)
			, ('flux-data-out', 		10000000000::BIGINT, 	0.0000000025::NUMERIC)
			, ('flux-data-out', 		100000000000::BIGINT,	0.0000000012::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)

			, ('oauth-client-creds', 	0::BIGINT, 				10::NUMERIC)
			, ('oauth-client-creds', 	100::BIGINT, 			5::NUMERIC)
			, ('oauth-client-creds', 	500::BIGINT, 			2.5::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2025-02-01'::DATE THEN
		RETURN QUERY SELECT *, '2024-11-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.00000575::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.00000345::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.00000092::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.00000023::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.000000115::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.000000046::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000005::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000002::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.0000000575::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.0000000115::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.00000000345::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.0000000023::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('flux-data-in', 			0::BIGINT, 				0.00000001::NUMERIC)
			, ('flux-data-in', 			1000000000::BIGINT, 	0.000000006::NUMERIC)
			, ('flux-data-in', 			10000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('flux-data-in', 			100000000000::BIGINT,	0.0000000015::NUMERIC)

			, ('flux-data-out', 		0::BIGINT, 				0.000000009::NUMERIC)
			, ('flux-data-out', 		1000000000::BIGINT, 	0.0000000055::NUMERIC)
			, ('flux-data-out', 		10000000000::BIGINT, 	0.0000000025::NUMERIC)
			, ('flux-data-out', 		100000000000::BIGINT,	0.0000000012::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)

			, ('oauth-client-creds', 	0::BIGINT, 				10::NUMERIC)
			, ('oauth-client-creds', 	100::BIGINT, 			5::NUMERIC)
			, ('oauth-client-creds', 	500::BIGINT, 			2.5::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSE
		RETURN QUERY SELECT *, '2025-02-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.00000575::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.00000345::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.00000092::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.00000023::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.000000115::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.000000046::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000005::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000002::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.0000000575::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.0000000115::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.00000000345::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.0000000023::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('flux-data-in', 			0::BIGINT, 				0.00000001::NUMERIC)
			, ('flux-data-in', 			1000000000::BIGINT, 	0.000000006::NUMERIC)
			, ('flux-data-in', 			10000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('flux-data-in', 			100000000000::BIGINT,	0.0000000015::NUMERIC)

			, ('flux-data-out', 		0::BIGINT, 				0.000000009::NUMERIC)
			, ('flux-data-out', 		1000000000::BIGINT, 	0.0000000055::NUMERIC)
			, ('flux-data-out', 		10000000000::BIGINT, 	0.0000000025::NUMERIC)
			, ('flux-data-out', 		100000000000::BIGINT,	0.0000000012::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)

			, ('oauth-client-creds', 	0::BIGINT, 				10::NUMERIC)
			, ('oauth-client-creds', 	100::BIGINT, 			5::NUMERIC)
			, ('oauth-client-creds', 	500::BIGINT, 			2.5::NUMERIC)

			, ('c2c-data', 				0::BIGINT, 				0.00000020::NUMERIC)
			, ('c2c-data', 				1000000000::BIGINT, 	0.00000009::NUMERIC)
			, ('c2c-data', 				10000000000::BIGINT, 	0.00000003::NUMERIC)
			, ('c2c-data', 				100000000000::BIGINT,	0.000000015::NUMERIC)
		) AS t(min, meter_key, cost);
	END IF;
END
$$;

CREATE OR REPLACE FUNCTION solarbill.billing_usage_tier_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		meter_key 	TEXT,
		tier_min 	BIGINT,
		tier_count 	BIGINT,
		tier_rate 	NUMERIC,
		tier_cost 	NUMERIC
	) LANGUAGE sql STABLE AS
$$
	WITH tiers AS (
		SELECT * FROM solarbill.billing_usage_tiers(effective_date)
	)
	, usage AS (
		SELECT
			  SUM(prop_in)::BIGINT AS prop_in
			, SUM(datum_stored)::BIGINT AS datum_stored
			, SUM(datum_out)::BIGINT AS datum_out
			, SUM(instr_issued)::BIGINT AS instr_issued
			, SUM(flux_data_in)::BIGINT AS flux_data_in
		FROM solarbill.billing_usage(userid, ts_min, ts_max)
	)
	, ocpp AS (
		SELECT count(*) AS ocpp_charger_count
		FROM solarev.ocpp_charge_point
		WHERE user_id = userid AND enabled = TRUE
	)
	, oscp AS (
		SELECT count(*) AS oscp_cap_group_count
		FROM solaroscp.oscp_cg_conf
		WHERE user_id = userid AND enabled = TRUE
	)
	, dnp3 AS (
		SELECT count(*) AS dnp3_data_point_count FROM (
			SELECT user_id, server_id, idx, 'm' AS dtype
			FROM solardnp3.dnp3_server_meas
			WHERE user_id = userid AND enabled = TRUE
			UNION ALL
			SELECT user_id, server_id, idx, 'c' AS dtype
			FROM solardnp3.dnp3_server_ctrl
			WHERE user_id = userid AND enabled = TRUE
		) counts
	)
	, oscp_cap AS (
		WITH oscp AS (
			-- extract datum stream + instantaneous properties from OSCP assets
			SELECT oac.node_id, oac.source_id, unnest(iprops) AS prop_name
			FROM solaroscp.oscp_asset_conf oac
			WHERE oac.user_id = userid AND enabled = TRUE
		)
		, m AS (
			-- extract stream ID and instantaneous property index from stream metadata
			SELECT m.stream_id
				, m.names_i
				, array_position(m.names_i, oscp.prop_name) AS prop_idx
				, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM oscp
			INNER JOIN solardatm.da_datm_meta m ON m.node_id = oscp.node_id AND m.source_id = oscp.source_id
			LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
			LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		)
		, d AS (
			-- extract maximum value seen on instantaneous stat for each stream + property
			SELECT d.stream_id, MAX(d.stat_i[m.prop_idx][3]) AS prop_max
			FROM m
			INNER JOIN solardatm.agg_datm_daily d ON d.stream_id = m.stream_id
			WHERE d.ts_start >= ts_min AT TIME ZONE m.time_zone
				AND d.ts_start < ts_max AT TIME ZONE m.time_zone
			GROUP BY d.stream_id

		)
		SELECT COALESCE(SUM(d.prop_max), 0)::BIGINT AS oscp_cap
		FROM d
	)
	, usvc AS (
		WITH tz AS (
			SELECT COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solaruser.user_user u
			LEFT OUTER JOIN solarnet.sn_loc l ON l.id = u.loc_id
			WHERE u.id = userid
		)
		SELECT
			  (SUM(a.cnt) FILTER (WHERE a.service = 'flxo'))::BIGINT AS flux_data_out
			, (SUM(a.cnt) FILTER (WHERE a.service = 'ccio'))::BIGINT AS c2c_data
		FROM solardatm.aud_user_daily a, tz
		WHERE a.user_id = userid
			AND a.ts_start >= ts_min AT TIME ZONE tz.time_zone
			AND a.ts_start < ts_max AT TIME ZONE tz.time_zone
	)
	, oauth AS (
		WITH cnts AS (
			SELECT COUNT(*) AS cnt
			FROM solaroscp.oscp_fp_token
			WHERE user_id = userid
				AND oauth = TRUE

			UNION ALL

			SELECT COUNT(*) AS cnt
			FROM solardin.inin_credential
			WHERE user_id = userid
				AND oauth = TRUE
		)
		SELECT SUM(a.cnt)::BIGINT AS oauth_client_creds_count
		FROM cnts a
	)
	SELECT
		  tiers.meter_key
		, tiers.min AS tier_min
		, LEAST(GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0), COALESCE(LEAD(tiers.min) OVER win - tiers.min, GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0))) AS tier_count
		, tiers.cost AS tier_rate
		, LEAST(GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0), COALESCE(LEAD(tiers.min) OVER win - tiers.min, GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0))) * tiers.cost AS tier_cost
	FROM usage n, ocpp, oscp, dnp3, oscp_cap, usvc, oauth
	CROSS JOIN tiers
	WINDOW win AS (PARTITION BY tiers.meter_key ORDER BY tiers.min)
$$;

DROP FUNCTION solarbill.billing_usage_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date DATE);

CREATE OR REPLACE FUNCTION solarbill.billing_usage_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date DATE DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		total_cost 						NUMERIC,
		prop_in 						BIGINT,
		prop_in_cost 					NUMERIC,
		prop_in_tiers 					NUMERIC[],
		prop_in_tiers_cost 				NUMERIC[],
		datum_stored 					BIGINT,
		datum_stored_cost 				NUMERIC,
		datum_stored_tiers 				NUMERIC[],
		datum_stored_tiers_cost 		NUMERIC[],
		datum_out 						BIGINT,
		datum_out_cost 					NUMERIC,
		datum_out_tiers 				NUMERIC[],
		datum_out_tiers_cost 			NUMERIC[],
		instr_issued 					BIGINT,
		instr_issued_cost 				NUMERIC,
		instr_issued_tiers 				NUMERIC[],
		instr_issued_tiers_cost 		NUMERIC[],
		flux_data_in 					BIGINT,
		flux_data_in_cost 				NUMERIC,
		flux_data_in_tiers 				NUMERIC[],
		flux_data_in_tiers_cost 		NUMERIC[],
		flux_data_out 					BIGINT,
		flux_data_out_cost 				NUMERIC,
		flux_data_out_tiers 			NUMERIC[],
		flux_data_out_tiers_cost 		NUMERIC[],
		ocpp_chargers					BIGINT,
		ocpp_chargers_cost				NUMERIC,
		ocpp_chargers_tiers				NUMERIC[],
		ocpp_chargers_tiers_cost		NUMERIC[],
		oscp_cap_groups					BIGINT,
		oscp_cap_groups_cost			NUMERIC,
		oscp_cap_groups_tiers			NUMERIC[],
		oscp_cap_groups_tiers_cost		NUMERIC[],
		dnp3_data_points				BIGINT,
		dnp3_data_points_cost			NUMERIC,
		dnp3_data_points_tiers			NUMERIC[],
		dnp3_data_points_tiers_cost		NUMERIC[],
		oscp_cap						BIGINT,
		oscp_cap_cost					NUMERIC,
		oscp_cap_tiers					NUMERIC[],
		oscp_cap_tiers_cost				NUMERIC[],
		oauth_client_creds				BIGINT,
		oauth_client_creds_cost			NUMERIC,
		oauth_client_creds_tiers		NUMERIC[],
		oauth_client_creds_tiers_cost	NUMERIC[],
		c2c_data						BIGINT,
		c2c_data_cost					NUMERIC,
		c2c_data_tiers					NUMERIC[],
		c2c_data_tiers_cost				NUMERIC[]
	) LANGUAGE sql STABLE AS
$$
	WITH tier_costs AS (
		SELECT * FROM solarbill.billing_usage_tier_details(userid, ts_min, ts_max, effective_date)
	)
	, costs AS (
		SELECT
			  meter_key
			, SUM(tier_count)::BIGINT AS total_count
			, SUM(tier_cost) AS total_cost
			, ARRAY_AGG(tier_count::NUMERIC) AS tier_counts
			, ARRAY_AGG(tier_cost) AS tier_costs
		FROM tier_costs
		WHERE tier_count > 0
		GROUP BY meter_key
	)
	SELECT
		  SUM(total_cost) AS total_cost

		, SUM(CASE meter_key WHEN 'datum-props-in' THEN total_count ELSE NULL END)::BIGINT AS prop_in
		, SUM(CASE meter_key WHEN 'datum-props-in' THEN total_cost ELSE NULL END) AS prop_in_cost
		, solarcommon.first(CASE meter_key WHEN 'datum-props-in' THEN tier_counts ELSE NULL END) AS prop_in_tiers
		, solarcommon.first(CASE meter_key WHEN 'datum-props-in' THEN tier_costs ELSE NULL END) AS prop_in_tiers_cost

		, SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_count ELSE NULL END)::BIGINT AS datum_stored
		, SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_cost ELSE NULL END) AS datum_stored_cost
		, solarcommon.first(CASE meter_key WHEN 'datum-days-stored' THEN tier_counts ELSE NULL END) AS datum_stored_tiers
		, solarcommon.first(CASE meter_key WHEN 'datum-days-stored' THEN tier_costs ELSE NULL END) AS datum_stored_tiers_cost

		, SUM(CASE meter_key WHEN 'datum-out' THEN total_count ELSE NULL END)::BIGINT AS datum_out
		, SUM(CASE meter_key WHEN 'datum-out' THEN total_cost ELSE NULL END) AS datum_out_cost
		, solarcommon.first(CASE meter_key WHEN 'datum-out' THEN tier_counts ELSE NULL END) AS datum_out_tiers
		, solarcommon.first(CASE meter_key WHEN 'datum-out' THEN tier_costs ELSE NULL END) AS datum_out_tiers_cost

		, SUM(CASE meter_key WHEN 'instr-issued' THEN total_count ELSE NULL END)::BIGINT AS instr_issued
		, SUM(CASE meter_key WHEN 'instr-issued' THEN total_cost ELSE NULL END) AS instr_issued_cost
		, solarcommon.first(CASE meter_key WHEN 'instr-issued' THEN tier_counts ELSE NULL END) AS instr_issued_tiers
		, solarcommon.first(CASE meter_key WHEN 'instr-issued' THEN tier_costs ELSE NULL END) AS instr_issued_tiers_cost

		, SUM(CASE meter_key WHEN 'flux-data-in' THEN total_count ELSE NULL END)::BIGINT AS flux_data_in
		, SUM(CASE meter_key WHEN 'flux-data-in' THEN total_cost ELSE NULL END) AS flux_data_in_cost
		, solarcommon.first(CASE meter_key WHEN 'flux-data-in' THEN tier_counts ELSE NULL END) AS flux_data_in_tiers
		, solarcommon.first(CASE meter_key WHEN 'flux-data-in' THEN tier_costs ELSE NULL END) AS flux_data_in_tiers_cost

		, SUM(CASE meter_key WHEN 'flux-data-out' THEN total_count ELSE NULL END)::BIGINT AS flux_data_out
		, SUM(CASE meter_key WHEN 'flux-data-out' THEN total_cost ELSE NULL END) AS flux_data_out_cost
		, solarcommon.first(CASE meter_key WHEN 'flux-data-out' THEN tier_counts ELSE NULL END) AS flux_data_out_tiers
		, solarcommon.first(CASE meter_key WHEN 'flux-data-out' THEN tier_costs ELSE NULL END) AS flux_data_out_tiers_cost

		, SUM(CASE meter_key WHEN 'ocpp-chargers' THEN total_count ELSE NULL END)::BIGINT AS ocpp_chargers
		, SUM(CASE meter_key WHEN 'ocpp-chargers' THEN total_cost ELSE NULL END) AS ocpp_chargers_cost
		, solarcommon.first(CASE meter_key WHEN 'ocpp-chargers' THEN tier_counts ELSE NULL END) AS ocpp_chargers_tiers
		, solarcommon.first(CASE meter_key WHEN 'ocpp-chargers' THEN tier_costs ELSE NULL END) AS ocpp_chargers_tiers_cost

		, SUM(CASE meter_key WHEN 'oscp-cap-groups' THEN total_count ELSE NULL END)::BIGINT AS oscp_cap_groups
		, SUM(CASE meter_key WHEN 'oscp-cap-groups' THEN total_cost ELSE NULL END) AS oscp_cap_groups_cost
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap-groups' THEN tier_counts ELSE NULL END) AS oscp_cap_groups_tiers
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap-groups' THEN tier_costs ELSE NULL END) AS oscp_cap_groups_tiers_cost

		, SUM(CASE meter_key WHEN 'dnp3-data-points' THEN total_count ELSE NULL END)::BIGINT AS dnp3_data_points
		, SUM(CASE meter_key WHEN 'dnp3-data-points' THEN total_cost ELSE NULL END) AS dnp3_data_points_cost
		, solarcommon.first(CASE meter_key WHEN 'dnp3-data-points' THEN tier_counts ELSE NULL END) AS dnp3_data_points_tiers
		, solarcommon.first(CASE meter_key WHEN 'dnp3-data-points' THEN tier_costs ELSE NULL END) AS dnp3_data_points_tiers_cost

		, SUM(CASE meter_key WHEN 'oscp-cap' THEN total_count ELSE NULL END)::BIGINT AS oscp_cap
		, SUM(CASE meter_key WHEN 'oscp-cap' THEN total_cost ELSE NULL END) AS oscp_cap_cost
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap' THEN tier_counts ELSE NULL END) AS oscp_cap_tiers
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap' THEN tier_costs ELSE NULL END) AS oscp_cap_tiers_cost

		, SUM(CASE meter_key WHEN 'oauth-client-creds' THEN total_count ELSE NULL END)::BIGINT AS oauth_client_creds
		, SUM(CASE meter_key WHEN 'oauth-client-creds' THEN total_cost ELSE NULL END) AS oauth_client_creds_cost
		, solarcommon.first(CASE meter_key WHEN 'oauth-client-creds' THEN tier_counts ELSE NULL END) AS oauth_client_creds_tiers
		, solarcommon.first(CASE meter_key WHEN 'oauth-client-creds' THEN tier_costs ELSE NULL END) AS oauth_client_creds_tiers_cost

		, SUM(CASE meter_key WHEN 'c2c-data' THEN total_count ELSE NULL END)::BIGINT AS c2c_data
		, SUM(CASE meter_key WHEN 'c2c-data' THEN total_cost ELSE NULL END) AS c2c_data_cost
		, solarcommon.first(CASE meter_key WHEN 'c2c-data' THEN tier_counts ELSE NULL END) AS c2c_data_tiers
		, solarcommon.first(CASE meter_key WHEN 'c2c-data' THEN tier_costs ELSE NULL END) AS c2c_data_tiers_cost
	FROM costs
	HAVING
		   SUM(CASE meter_key WHEN 'datum-props-in' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'datum-out' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'instr-issued' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'flux-data-in' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'flux-data-out' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'ocpp-chargers' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'oscp-cap-groups' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'dnp3-data-points' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'oscp-cap' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'oauth-client-creds' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'c2c-data' THEN total_count ELSE NULL END)::BIGINT > 0
$$;
